package com.bridgenote.realtime.dto;

/**
 * 서버 → 클라이언트 경고 메시지(비동기). (JSON snake_case: sentence_id, risk_level, note_type)
 */
public record WarningMessage(
		String type,
		String sentenceId,
		String riskLevel,
		String noteType
) {
	public static WarningMessage of(String sentenceId, String riskLevel, String noteType) {
		return new WarningMessage("warning", sentenceId, riskLevel, noteType);
	}
}
