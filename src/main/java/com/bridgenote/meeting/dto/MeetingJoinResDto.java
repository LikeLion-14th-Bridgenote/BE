package com.bridgenote.meeting.dto;

/**
 * 회의 참가 응답 (200). (JSON snake_case: participant_id, meeting_id, speaker_index)
 * profile 표준 필드: language / culture / job (팀 도메인 합의).
 */
public record MeetingJoinResDto(
		String participantId,
		String meetingId,
		Integer speakerIndex,
		ProfileDto profile
) {
	public record ProfileDto(
			String nickname,
			String language,
			String culture,
			String job
	) {
	}
}
