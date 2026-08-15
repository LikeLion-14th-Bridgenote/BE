package com.bridgenote.realtime.service;

import com.bridgenote.ai.client.AiAnalyzeClient;
import com.bridgenote.ai.dto.AnalyzeRequest;
import com.bridgenote.ai.dto.AnalyzeResponse;
import com.bridgenote.participant.domain.Participant;
import com.bridgenote.participant.repository.ParticipantRepository;
import com.bridgenote.realtime.domain.CulturalNote;
import com.bridgenote.realtime.domain.UtteranceTranslation;
import com.bridgenote.realtime.dto.TranslationMessage;
import com.bridgenote.realtime.dto.WarningMessage;
import com.bridgenote.realtime.repository.CulturalNoteRepository;
import com.bridgenote.realtime.repository.UtteranceTranslationRepository;
import com.bridgenote.realtime.session.WebSocketSessionRegistry;
import com.bridgenote.user.domain.User;
import com.bridgenote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 확정 발화(is_final) → AI /ai/analyze 오케스트레이션.
 * <ol>
 *   <li>회의 참가자에서 현재 화자/청자를 뽑아 문화권·직무·언어를 조립(User는 authUserId=profile_id로 조회)</li>
 *   <li>{@link AiAnalyzeClient}로 비동기 호출</li>
 *   <li>응답을 청자 언어별 {@link TranslationMessage}, 오해 소지 있으면 {@link WarningMessage}로 브로드캐스트</li>
 * </ol>
 * 화자 혼자면(청자 0) 분석/번역이 불필요하므로 호출하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UtteranceAnalyzer {

	private final ParticipantRepository participantRepository;
	private final UserRepository userRepository;
	private final AiAnalyzeClient aiAnalyzeClient;
	private final WebSocketSessionRegistry sessionRegistry;
	// 회의록(전사·문화 가이드) 재열람을 위한 저장소
	private final CulturalNoteRepository culturalNoteRepository;
	private final UtteranceTranslationRepository utteranceTranslationRepository;

	/** 확정 발화 1건을 AI에 분석 요청하고, 결과(번역·경고)를 회의 전원에게 push한다. */
	public void analyzeAndBroadcast(String meetingId, String sentenceId,
									String sourceLang, String sourceText, Integer speakerIndex) {
		List<Participant> joined = participantRepository
				.findByMeetingIdAndSpeakerIndexIsNotNullOrderBySpeakerIndexAsc(meetingId);
		if (joined.size() < 2) {
			return; // 청자가 없으면(혼자) 번역·분석 불필요
		}

		Participant speaker = joined.stream()
				.filter(p -> Objects.equals(p.getSpeakerIndex(), speakerIndex))
				.findFirst()
				.orElse(null);
		User speakerUser = (speaker == null) ? null
				: userRepository.findByAuthUserId(speaker.getProfileId()).orElse(null);

		List<AnalyzeRequest.Listener> listeners = new ArrayList<>();
		for (Participant p : joined) {
			if (speaker != null && p.getId().equals(speaker.getId())) {
				continue; // 화자 본인 제외
			}
			User u = userRepository.findByAuthUserId(p.getProfileId()).orElse(null);
			String lang = (p.getLanguage() != null) ? p.getLanguage() : langOf(u);
			listeners.add(new AnalyzeRequest.Listener(p.getProfileId(), lang, cultureOf(u), jobOf(u)));
		}
		if (listeners.isEmpty()) {
			return;
		}

		AnalyzeRequest request = new AnalyzeRequest(
				sentenceId,
				meetingId,
				sourceText,
				sourceLang,
				null,                       // meeting_context: 현재 미사용
				new AnalyzeRequest.Speaker(cultureOf(speakerUser), jobOf(speakerUser)),
				listeners,
				List.of());                 // context: 직전 발화 미전달(1차)

		aiAnalyzeClient.analyze(request, resp -> onAnalyzed(meetingId, speakerIndex, resp));
	}

	/** AI 응답을 저장(전사 번역·각주)하고 WS 메시지로 브로드캐스트한다. (비동기 콜백) */
	private void onAnalyzed(String meetingId, Integer speakerIndex, AnalyzeResponse resp) {
		if (resp.translations() != null) {
			// 번역은 언어 단위(클라가 자기 언어만 표시). 같은 lang은 1회만, 빈 텍스트는 스킵.
			Set<String> langs = new HashSet<>();
			for (AnalyzeResponse.Translation t : resp.translations()) {
				if (t.text() == null || t.text().isBlank() || !langs.add(t.lang())) {
					continue;
				}
				saveTranslation(meetingId, resp.sentenceId(), t.lang(), t.text());
				sessionRegistry.broadcast(meetingId,
						TranslationMessage.of(resp.sentenceId(), t.lang(), t.text()));
			}
		}

		if (resp.hasRisk()) {
			saveCulturalNote(meetingId, speakerIndex, resp);
			sessionRegistry.broadcast(meetingId, WarningMessage.from(resp));
		}
	}

	/** 발화 번역 저장(중복 lang은 스킵). 저장 실패해도 실시간 파이프라인엔 영향 없도록 방어. */
	private void saveTranslation(String meetingId, String sentenceId, String lang, String text) {
		try {
			if (utteranceTranslationRepository.existsBySentenceIdAndLang(sentenceId, lang)) {
				return;
			}
			utteranceTranslationRepository.save(UtteranceTranslation.builder()
					.meetingId(meetingId).sentenceId(sentenceId).lang(lang).text(text).build());
		} catch (RuntimeException e) {
			log.warn("번역 저장 실패 sentence={} lang={}: {}", sentenceId, lang, e.toString());
		}
	}

	/** 문화 각주 저장(발화당 1건). */
	private void saveCulturalNote(String meetingId, Integer speakerIndex, AnalyzeResponse resp) {
		try {
			if (culturalNoteRepository.existsBySentenceId(resp.sentenceId())) {
				return;
			}
			culturalNoteRepository.save(CulturalNote.builder()
					.meetingId(meetingId).sentenceId(resp.sentenceId()).speakerIndex(speakerIndex)
					.riskLevel(resp.riskLevel()).noteType(resp.noteType())
					.speakerIntent(resp.speakerIntent()).listenerMisread(resp.listenerMisread())
					.advice(resp.advice()).rewriteText(resp.rewriteText()).build());
		} catch (RuntimeException e) {
			log.warn("각주 저장 실패 sentence={}: {}", resp.sentenceId(), e.toString());
		}
	}

	private String cultureOf(User u) {
		return (u != null && u.getCulture() != null) ? u.getCulture() : "";
	}

	private String jobOf(User u) {
		return (u != null && u.getJob() != null) ? u.getJob() : "";
	}

	private String langOf(User u) {
		return (u != null && u.getLanguage() != null) ? u.getLanguage() : "";
	}
}
