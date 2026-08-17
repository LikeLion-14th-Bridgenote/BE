package com.bridgenote.realtime.service;

import com.bridgenote.participant.domain.Participant;
import com.bridgenote.participant.repository.ParticipantRepository;
import com.bridgenote.realtime.domain.Utterance;
import com.bridgenote.realtime.dto.CaptionMessage;
import com.bridgenote.realtime.repository.UtteranceRepository;
import com.bridgenote.realtime.session.MeetingSpeakerState;
import com.bridgenote.realtime.session.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 오디오 청크 → Deepgram STT 오케스트레이션.
 * <ol>
 *   <li>audio_chunk를 디코드해 Deepgram으로 스트리밍({@link DeepgramConnectionManager})</li>
 *   <li>전사 결과({@link #onTranscript})가 오면 자막을 즉시 브로드캐스트하고, 확정(is_final)이면 utterance 저장</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SttService {

	private final DeepgramConnectionManager deepgram;
	private final WebSocketSessionRegistry sessionRegistry;
	private final MeetingSpeakerState speakerState;
	private final UtteranceRepository utteranceRepository;
	private final UtteranceAnalyzer utteranceAnalyzer;
	private final ParticipantRepository participantRepository;
	// 회의별 화자번호 → 언어 캐시(오디오 청크마다 DB 조회 방지).
	private final Map<String, Map<Integer, String>> speakerLangCache = new ConcurrentHashMap<>();
	// 미스 재조회 쿨다운(회의별 마지막 재조회 시각). 영구 미스(언어 null·미참가 화자)가 청크마다 DB를 치는 것 방지.
	private final Map<String, Long> speakerLangReloadAt = new ConcurrentHashMap<>();
	private static final long SPEAKER_LANG_RELOAD_COOLDOWN_MS = 5000;

	/** WS 핸들러가 audio_chunk 수신 시 호출. */
	public void submitAudio(String meetingId, Integer speakerIndex, Long seq, String base64Data) {
		if (speakerIndex != null) {
			speakerState.set(meetingId, speakerIndex);
		}

		byte[] audio;
		try {
			audio = (base64Data == null) ? new byte[0] : Base64.getDecoder().decode(base64Data);
		} catch (IllegalArgumentException e) {
			log.warn("audio_chunk base64 디코드 실패 meeting={} seq={}", meetingId, seq);
			return;
		}

		// 현재 화자의 언어로 Deepgram 연결 선택(KR 화자는 ko, VN 화자는 vi로 전사 — multi 뭉갬 회피)
		String lang = speakerLang(meetingId, speakerState.get(meetingId));
		deepgram.sendAudio(meetingId, lang, audio,
				(sentenceId, sourceLang, text, isFinal) ->
						onTranscript(meetingId, sentenceId, sourceLang, text, isFinal));
	}

	/**
	 * 화자 번호 → 언어(participant.language). 캐시에서 찾되, 미스면 재조회해 갱신한다.
	 * (호스트가 먼저 말해 캐시가 굳으면, 이후 합류한 화자가 전부 기본 언어로 떨어지던 문제 방지.)
	 * 재조회는 새 화자가 나타난 첫 청크에서만 일어나므로 청크마다 DB를 치지 않는다.
	 */
	private String speakerLang(String meetingId, Integer speakerIndex) {
		if (speakerIndex == null) {
			return null;
		}
		Map<Integer, String> byIndex = speakerLangCache.computeIfAbsent(meetingId, this::loadSpeakerLangs);
		String lang = byIndex.get(speakerIndex);
		if (lang == null && reloadDue(meetingId)) {
			// 캐시 이후 합류 등 → 재조회 후 교체. 쿨다운으로 영구 미스가 청크마다 DB 치는 것 방지.
			byIndex = loadSpeakerLangs(meetingId);
			speakerLangCache.put(meetingId, byIndex);
			speakerLangReloadAt.put(meetingId, System.currentTimeMillis());
			lang = byIndex.get(speakerIndex);
		}
		return lang;
	}

	private boolean reloadDue(String meetingId) {
		Long last = speakerLangReloadAt.get(meetingId);
		return last == null || System.currentTimeMillis() - last >= SPEAKER_LANG_RELOAD_COOLDOWN_MS;
	}

	private Map<Integer, String> loadSpeakerLangs(String meetingId) {
		return participantRepository.findByMeetingIdAndSpeakerIndexIsNotNullOrderBySpeakerIndexAsc(meetingId).stream()
				.filter(p -> p.getSpeakerIndex() != null && p.getLanguage() != null)
				.collect(Collectors.toConcurrentMap(
						Participant::getSpeakerIndex, Participant::getLanguage, (a, b) -> a));
	}

	/**
	 * Deepgram 전사 결과 처리: 자막 즉시 브로드캐스트, 확정(is_final)이면 utterance 저장.
	 * (화자는 수신 시점의 현재 화자 상태로 태깅)
	 */
	public void onTranscript(String meetingId, String sentenceId, String sourceLang, String text, boolean isFinal) {
		Integer speakerIndex = speakerState.get(meetingId);

		// 자막은 즉시 브로드캐스트(interim 포함)
		sessionRegistry.broadcast(meetingId,
				CaptionMessage.of(sentenceId, speakerIndex, sourceLang, text, isFinal));

		if (isFinal) {
			utteranceRepository.save(Utterance.builder()
					.sentenceId(sentenceId)
					.meetingId(meetingId)
					.speakerIndex(speakerIndex)
					.sourceLang(sourceLang)
					.sourceText(text)
					.isFinal(true)
					.spokenAt(Instant.now())
					.build());
			// AI /ai/analyze 비동기 호출 → translation·warning 브로드캐스트 (실패해도 자막 파이프라인엔 영향 없음)
			utteranceAnalyzer.analyzeAndBroadcast(meetingId, sentenceId, sourceLang, text, speakerIndex);
		}
	}

	/** 회의 종료/마지막 참가자 퇴장 시 정리. */
	public void closeMeeting(String meetingId) {
		deepgram.close(meetingId);
		speakerState.clear(meetingId);
		speakerLangCache.remove(meetingId);
		speakerLangReloadAt.remove(meetingId);
	}
}
