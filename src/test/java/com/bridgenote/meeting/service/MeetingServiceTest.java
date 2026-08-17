package com.bridgenote.meeting.service;

import com.bridgenote.common.jwt.AuthUser;
import com.bridgenote.meeting.domain.Meeting;
import com.bridgenote.meeting.domain.MeetingStatus;
import com.bridgenote.meeting.dto.MeetingDetailResDto;
import com.bridgenote.meeting.dto.MeetingJoinReqDto;
import com.bridgenote.meeting.dto.MeetingJoinResDto;
import com.bridgenote.meeting.repository.MeetingRepository;
import com.bridgenote.participant.domain.Participant;
import com.bridgenote.participant.repository.ParticipantRepository;
import com.bridgenote.realtime.session.WebSocketSessionRegistry;
import com.bridgenote.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

	private static final String MEETING_ID = "meeting-1";
	private static final String PROFILE_ID = "profile-1";

	@Mock
	private MeetingRepository meetingRepository;
	@Mock
	private ParticipantRepository participantRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private WebSocketSessionRegistry sessionRegistry;
	@Mock
	private MinutesGenerator minutesGenerator;

	@InjectMocks
	private MeetingService meetingService;

	@Test
	void joinedParticipantStartsWaitingMeeting() {
		Meeting meeting = waitingMeeting();
		Participant participant = joinedParticipant();
		when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));
		when(participantRepository.findByMeetingIdAndProfileId(MEETING_ID, PROFILE_ID))
				.thenReturn(Optional.of(participant));

		MeetingService.ConnectResult result = meetingService.onParticipantConnect(MEETING_ID, PROFILE_ID);

		assertEquals(MeetingService.ConnectResult.STARTED, result);
		assertEquals(MeetingStatus.LIVE, meeting.getStatus());
		verify(participantRepository).findByMeetingIdAndProfileId(MEETING_ID, PROFILE_ID);
	}

	@Test
	void participantWithoutJoinCannotStartMeeting() {
		Meeting meeting = waitingMeeting();
		Participant consentOnlyParticipant = Participant.builder()
				.meetingId(MEETING_ID)
				.profileId(PROFILE_ID)
				.build();
		consentOnlyParticipant.applyConsent(true, Instant.now());
		when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));
		when(participantRepository.findByMeetingIdAndProfileId(MEETING_ID, PROFILE_ID))
				.thenReturn(Optional.of(consentOnlyParticipant));

		MeetingService.ConnectResult result = meetingService.onParticipantConnect(MEETING_ID, PROFILE_ID);

		assertEquals(MeetingService.ConnectResult.NOT_JOINED, result);
		assertEquals(MeetingStatus.WAITING, meeting.getStatus());
	}

	@Test
	void successfulJoinMakesParticipantVisibleInMeetingDetail() {
		Meeting meeting = waitingMeeting();
		Participant participant = Participant.builder()
				.meetingId(MEETING_ID)
				.profileId(PROFILE_ID)
				.build();
		participant.applyConsent(true, Instant.now());
		when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));
		when(participantRepository.findByMeetingIdAndProfileId(MEETING_ID, PROFILE_ID))
				.thenReturn(Optional.of(participant));
		when(participantRepository.countByMeetingIdAndSpeakerIndexIsNotNull(MEETING_ID)).thenReturn(0L);
		when(userRepository.findByAuthUserId(PROFILE_ID)).thenReturn(Optional.empty());

		MeetingJoinResDto joinResponse = meetingService.join(
				new AuthUser(PROFILE_ID, "user@example.com"),
				MEETING_ID,
				new MeetingJoinReqDto("ABC123"));
		when(participantRepository.findByMeetingIdAndSpeakerIndexIsNotNullOrderBySpeakerIndexAsc(MEETING_ID))
				.thenReturn(List.of(participant));

		MeetingDetailResDto detail = meetingService.getMeeting(MEETING_ID);

		assertEquals(0, joinResponse.speakerIndex());
		assertEquals(1, detail.participants().size());
		assertEquals(PROFILE_ID, detail.participants().getFirst().profileId());
		assertEquals(0, detail.participants().getFirst().speakerIndex());
	}

	private Meeting waitingMeeting() {
		return Meeting.builder()
				.hostId(PROFILE_ID)
				.title("테스트 회의")
				.inviteCode("ABC123")
				.build();
	}

	private Participant joinedParticipant() {
		Participant participant = Participant.builder()
				.meetingId(MEETING_ID)
				.profileId(PROFILE_ID)
				.build();
		participant.applyConsent(true, Instant.now());
		participant.completeJoin("참가자", "ko", 0, Instant.now());
		return participant;
	}
}
