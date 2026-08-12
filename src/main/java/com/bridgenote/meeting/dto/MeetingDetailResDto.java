package com.bridgenote.meeting.dto;

import com.bridgenote.meeting.domain.Meeting;
import com.bridgenote.participant.dto.ParticipantResDto;

import java.util.List;

/**
 * 회의 상세 응답 (200) — 회의 정보 + 참가자 목록.
 */
public record MeetingDetailResDto(
		String id,
		String title,
		String status,
		List<ParticipantResDto> participants
) {
	public static MeetingDetailResDto of(Meeting meeting, List<ParticipantResDto> participants) {
		return new MeetingDetailResDto(
				meeting.getId(),
				meeting.getTitle(),
				meeting.getStatus().name().toLowerCase(),
				participants
		);
	}
}
