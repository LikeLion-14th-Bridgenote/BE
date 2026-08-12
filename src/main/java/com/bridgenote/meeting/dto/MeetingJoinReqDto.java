package com.bridgenote.meeting.dto;

/**
 * 회의 참가 요청. (JSON: invite_code)
 * 초대 코드 검증은 서비스에서 수행(누락/불일치 → 400).
 */
public record MeetingJoinReqDto(
		String inviteCode
) {
}
