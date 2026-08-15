package com.bridgenote.ai.dto;

import java.util.List;

/**
 * POST /ai/minutes 응답 (AI → BE). JSON snake_case.
 * 언어·직무별 섹션(결정/논의/액션)으로 구성된다.
 */
public record MinutesResponse(
		String meetingId,
		List<Section> minutes
) {
	/** 언어·직무별 회의록 한 섹션. */
	public record Section(
			String language,
			String jobRole,
			List<String> decisions,
			List<String> discussions,
			List<ActionItem> actionItems
	) {
	}

	/** 액션 아이템. owner(담당자)·deadline(기한)은 FE 액션 탭 컬럼용(없을 수 있음). */
	public record ActionItem(String task, String owner, String deadline) {
	}
}
