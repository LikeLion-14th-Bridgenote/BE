package com.bridgenote.meeting.dto;

import com.bridgenote.meeting.domain.Meeting;

import java.time.Instant;

/**
 * 내 회의 목록 항목 (200). (JSON snake_case: participant_count, started_at, ended_at)
 */
public record MeetingListResDto(
		String id,
		String title,
		String status,
		long participantCount,
		Instant startedAt,
		Instant endedAt
) {
	public static MeetingListResDto of(Meeting meeting, long participantCount) {
		return new MeetingListResDto(
				meeting.getId(),
				meeting.getTitle(),
				meeting.getStatus().name().toLowerCase(),
				participantCount,
				meeting.getStartedAt(),
				meeting.getEndedAt()
		);
	}
}
