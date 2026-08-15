package com.bridgenote.ai.dto;

import java.util.List;

/**
 * POST /ai/minutes 요청 (BE → AI, 배치). 회의 종료 시 확정 발화 전체를 담아 호출한다.
 * JSON snake_case: meeting_id, job_roles, sentence_id.
 */
public record MinutesRequest(
		String meetingId,
		List<String> languages,
		List<String> jobRoles,
		List<Utterance> utterances
) {
	/** 회의록 재료: 확정 발화 1건. speaker는 표시용(선택). */
	public record Utterance(String sentenceId, String speaker, String lang, String text) {
	}
}
