package com.bridgenote.ai.dto;

import java.util.List;

/**
 * POST /ai/analyze 응답 (AI → BE). JSON snake_case.
 * <ul>
 *   <li>게이트 통과(has_risk=true): 각주 필드 + translations</li>
 *   <li>게이트 미통과(has_risk=false): 각주 필드는 null(AI가 제외) + translations만</li>
 * </ul>
 */
public record AnalyzeResponse(
		String sentenceId,
		boolean hasRisk,
		String riskLevel,
		String noteType,
		String speakerIntent,
		String listenerMisread,
		String advice,
		String rewriteText,
		List<Translation> translations
) {
	/** 청자 언어별 번역. participant_id는 요청의 listener와 대응. */
	public record Translation(String participantId, String lang, String text) {
	}
}
