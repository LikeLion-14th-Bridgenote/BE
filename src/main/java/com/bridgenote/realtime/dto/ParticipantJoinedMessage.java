package com.bridgenote.realtime.dto;

import com.bridgenote.participant.domain.Participant;

/**
 * 서버 → 클라: 참가자 입장 알림. (JSON snake_case: profile_id, speaker_index)
 */
public record ParticipantJoinedMessage(
		String type,
		String profileId,
		Integer speakerIndex,
		String nickname,
		String language
) {
	public static ParticipantJoinedMessage of(Participant p) {
		return new ParticipantJoinedMessage(
				"participant_joined", p.getProfileId(), p.getSpeakerIndex(), p.getNickname(), p.getLanguage());
	}
}
