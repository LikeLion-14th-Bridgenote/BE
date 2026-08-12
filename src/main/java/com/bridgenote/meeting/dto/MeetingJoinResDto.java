package com.bridgenote.meeting.dto;

/**
 * 회의 참가 응답 (200). (JSON snake_case: participant_id, meeting_id, speaker_index, job_role)
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
			String jobRole
	) {
	}
}
