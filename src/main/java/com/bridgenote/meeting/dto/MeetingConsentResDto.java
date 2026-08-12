package com.bridgenote.meeting.dto;

import java.time.Instant;

/**
 * 데이터 처리 동의 응답 (200). (JSON snake_case: meeting_id, consented_at)
 */
public record MeetingConsentResDto(
		String meetingId,
		boolean consented,
		Instant consentedAt
) {
}
