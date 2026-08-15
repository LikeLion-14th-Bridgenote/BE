package com.bridgenote.realtime.dto;

import com.bridgenote.ai.dto.AnalyzeResponse;

/**
 * 서버 → 클라이언트 경고 메시지(비동기). 회의 진행 화면 "문화 경고" 패널을 채운다.
 * (JSON snake_case: sentence_id, risk_level, note_type, speaker_intent, listener_misread, rewrite_text)
 */
public record WarningMessage(
		String type,
		String sentenceId,
		String riskLevel,
		String noteType,
		String speakerIntent,
		String listenerMisread,
		String advice,
		String rewriteText
) {
	public static WarningMessage from(AnalyzeResponse r) {
		return new WarningMessage("warning", r.sentenceId(), r.riskLevel(), r.noteType(),
				r.speakerIntent(), r.listenerMisread(), r.advice(), r.rewriteText());
	}
}
