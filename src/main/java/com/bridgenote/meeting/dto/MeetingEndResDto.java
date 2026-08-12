package com.bridgenote.meeting.dto;

import com.bridgenote.meeting.domain.Meeting;

import java.time.Instant;

/**
 * 회의 종료 응답 (200). (JSON snake_case: ended_at)
 */
public record MeetingEndResDto(
		String id,
		String status,
		Instant endedAt
) {
	public static MeetingEndResDto from(Meeting meeting) {
		return new MeetingEndResDto(
				meeting.getId(),
				meeting.getStatus().name().toLowerCase(),
				meeting.getEndedAt()
		);
	}
}
