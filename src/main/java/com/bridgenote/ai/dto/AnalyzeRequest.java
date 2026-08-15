package com.bridgenote.ai.dto;

import java.util.List;

/**
 * POST /ai/analyze 요청 (BE → AI). 확정 발화(is_final) 1건을 담아 호출한다.
 * JSON snake_case: sentence_id, source_text, source_lang, meeting_context, participant_id.
 * 계약 표준: speaker/listener의 직무 필드는 {@code job}, 발화 식별자는 {@code sentence_id}.
 */
public record AnalyzeRequest(
		String sentenceId,
		String meetingId,
		String sourceText,
		String sourceLang,
		String meetingContext,
		Speaker speaker,
		List<Listener> listeners,
		List<ContextItem> context
) {
	/** 화자 프로필(문화권·직무). */
	public record Speaker(String culture, String job) {
	}

	/** 청자 프로필(번역 타겟 언어 + 문화권·직무). participant_id로 번역이 매핑된다. */
	public record Listener(String participantId, String lang, String culture, String job) {
	}

	/** 직전 맥락 발화(선택). */
	public record ContextItem(String text, String lang) {
	}
}
