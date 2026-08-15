package com.bridgenote.realtime.dto;

/**
 * 서버 → 클라: 참가자 퇴장 알림. (JSON snake_case: profile_id)
 */
public record ParticipantLeftMessage(
		String type,
		String profileId
) {
	public static ParticipantLeftMessage of(String profileId) {
		return new ParticipantLeftMessage("participant_left", profileId);
	}
}
