package com.bridgenote.meeting.dto;

import java.time.Instant;
import java.util.List;

/**
 * GET /api/meetings/{id}/utterances 응답 (전체 전사 기록, 페이지네이션).
 * 각 행: 시간·발화자·원문·번역문·sentence_id.
 */
public record TranscriptResDto(
		String meetingId,
		int page,
		int size,
		int totalPages,
		long totalElements,
		List<Row> utterances
) {
	/** 발화 1건 + 언어별 번역. */
	public record Row(
			String sentenceId,
			Integer speakerIndex,
			String speaker,
			Instant spokenAt,
			String sourceLang,
			String sourceText,
			List<Translation> translations
	) {
	}

	public record Translation(String lang, String text) {
	}
}
