package com.bridgenote.participant.dto;

import com.bridgenote.participant.domain.Participant;

/**
 * 회의 참가자 표현. (JSON snake_case: profile_id, speaker_index)
 */
public record ParticipantResDto(
		String profileId,
		String nickname,
		String language,
		Integer speakerIndex
) {
	public static ParticipantResDto from(Participant p) {
		return new ParticipantResDto(p.getProfileId(), p.getNickname(), p.getLanguage(), p.getSpeakerIndex());
	}
}
