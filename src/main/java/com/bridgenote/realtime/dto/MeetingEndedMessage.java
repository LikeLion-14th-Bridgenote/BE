package com.bridgenote.realtime.dto;

import java.time.Instant;

/**
 * 서버 → 클라: 회의 종료 알림(주최자 /end 시). (JSON snake_case: meeting_id, ended_at)
 */
public record MeetingEndedMessage(
		String type,
		String meetingId,
		Instant endedAt
) {
	public static MeetingEndedMessage of(String meetingId, Instant endedAt) {
		return new MeetingEndedMessage("meeting_ended", meetingId, endedAt);
	}
}
