package com.bridgenote.meeting.dto;

import com.bridgenote.ai.dto.MinutesResponse;

import java.util.List;

/**
 * GET /api/meetings/{id}/minutes 응답.
 * status: pending(생성 중) | ready(완료) | failed(생성 실패). ready일 때만 minutes가 채워진다.
 * minutes 섹션 형태는 AI 응답 그대로(pass-through).
 */
public record MinutesResultResDto(
		String meetingId,
		String status,
		List<MinutesResponse.Section> minutes
) {
	public static MinutesResultResDto of(String meetingId, String status, List<MinutesResponse.Section> minutes) {
		return new MinutesResultResDto(meetingId, status, minutes);
	}
}
