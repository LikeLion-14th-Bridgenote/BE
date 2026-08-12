package com.bridgenote.realtime.service;

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

		deepgram.sendAudio(meetingId, audio,
				(sentenceId, sourceLang, text, isFinal) ->
						onTranscript(meetingId, sentenceId, sourceLang, text, isFinal));
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
			// TODO(2): AI 서버 /ai/analyze 호출 → translation·warning 브로드캐스트 + 저장
		}
	}

	/** 회의 종료/마지막 참가자 퇴장 시 정리. */
	public void closeMeeting(String meetingId) {
		deepgram.close(meetingId);
		speakerState.clear(meetingId);
	}
}
