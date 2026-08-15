package com.bridgenote.meeting.dto;

import com.bridgenote.meeting.domain.Meeting;

import java.time.Instant;

/**
 * 회의 생성 응답 (201). (JSON은 snake_case: invite_code, invite_url, expected_count, created_at)
 */
public record MeetingCreateResDto(
		String id,
		String title,
		String description,
		Integer expectedCount,
		String inviteCode,
		String inviteUrl,
		String status,
		Instant createdAt
) {
	public static MeetingCreateResDto from(Meeting meeting, String inviteUrl) {
		return new MeetingCreateResDto(
				meeting.getId(),
				meeting.getTitle(),
				meeting.getDescription(),
				meeting.getExpectedCount(),
				meeting.getInviteCode(),
				inviteUrl,
				meeting.getStatus().name().toLowerCase(),
				meeting.getCreatedAt()
		);
	}
}
