package com.bridgenote.realtime.dto;

import java.time.Instant;

/**
 * 서버 → 클라: 회의 시작 알림(첫 참가자 WS 접속으로 WAITING→LIVE 전환 시).
 * (JSON snake_case: meeting_id, started_at)
 */
public record MeetingStartedMessage(
		String type,
		String meetingId,
		Instant startedAt
) {
	public static MeetingStartedMessage of(String meetingId, Instant startedAt) {
		return new MeetingStartedMessage("meeting_started", meetingId, startedAt);
	}
}
