package com.bridgenote.meeting.dto;

import java.time.Instant;
import java.util.List;

/**
 * GET /api/meetings/{id}/cultural-notes 응답 (문화 가이드 탭).
 * 전체 각주 수 + note_type별 집계 + 각주 목록.
 */
public record CulturalNoteResDto(
		String meetingId,
		long total,
		List<TypeCount> byType,
		List<Note> notes
) {
	/** note_type별 각주 개수 (문화 이해 / 커뮤니케이션 / 업무 스타일). */
	public record TypeCount(String noteType, long count) {
	}

	/** 각주 1건 (발화자·위험도·3요소·리라이트). */
	public record Note(
			String sentenceId,
			Integer speakerIndex,
			String speaker,
			String riskLevel,
			String noteType,
			String speakerIntent,
			String listenerMisread,
			String advice,
			String rewriteText,
			Instant createdAt
	) {
	}
}
