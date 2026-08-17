package com.bridgenote.realtime.handler;

import com.bridgenote.common.jwt.AuthUser;
import com.bridgenote.meeting.service.MeetingService;
import com.bridgenote.participant.domain.Participant;
import com.bridgenote.participant.repository.ParticipantRepository;
import com.bridgenote.realtime.dto.ParticipantJoinedMessage;
import com.bridgenote.realtime.dto.ParticipantLeftMessage;
import com.bridgenote.realtime.service.SttService;
import com.bridgenote.realtime.session.MeetingSpeakerState;
import com.bridgenote.realtime.session.WebSocketSessionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingWebSocketHandlerTest {

	private static final String MEETING_ID = "meeting-1";
	private static final String PROFILE_ID = "profile-1";

	@Mock
	private MeetingService meetingService;
	@Mock
	private ParticipantRepository participantRepository;
	@Mock
	private WebSocketSessionRegistry sessionRegistry;
	@Mock
	private MeetingSpeakerState speakerState;
	@Mock
	private SttService sttService;
	@Mock
	private ObjectMapper objectMapper;
	@Mock
	private WebSocketSession session;

	private MeetingWebSocketHandler handler;
	private Map<String, Object> attributes;

	@BeforeEach
	void setUp() {
		handler = new MeetingWebSocketHandler(
				meetingService, participantRepository, sessionRegistry, speakerState, sttService, objectMapper);
		attributes = new HashMap<>();
		attributes.put(AuthHandshakeInterceptor.ATTR_AUTH_USER, new AuthUser(PROFILE_ID, "user@example.com"));
		attributes.put(AuthHandshakeInterceptor.ATTR_MEETING_ID, MEETING_ID);
		when(session.getAttributes()).thenReturn(attributes);
	}

	@Test
	void joinedParticipantIsRegisteredBeforeJoinedEventBroadcast() throws Exception {
		Participant participant = joinedParticipant();
		when(meetingService.onParticipantConnect(MEETING_ID, PROFILE_ID))
				.thenReturn(MeetingService.ConnectResult.LIVE);
		when(participantRepository.findByMeetingIdAndProfileId(MEETING_ID, PROFILE_ID))
				.thenReturn(Optional.of(participant));

		handler.afterConnectionEstablished(session);

		assertTrue(Boolean.TRUE.equals(attributes.get(MeetingWebSocketHandler.ATTR_JOINED_PARTICIPANT)));
		InOrder order = inOrder(sessionRegistry);
		order.verify(sessionRegistry).add(MEETING_ID, session);
		order.verify(sessionRegistry).broadcast(eq(MEETING_ID), any(ParticipantJoinedMessage.class));
		verify(sessionRegistry).broadcast(eq(MEETING_ID), argThat(payload ->
				payload instanceof ParticipantJoinedMessage message
						&& PROFILE_ID.equals(message.profileId())
						&& Integer.valueOf(0).equals(message.speakerIndex())));
	}

	@Test
	void connectionIsRejectedWhenJoinWasNotCompleted() throws Exception {
		when(meetingService.onParticipantConnect(MEETING_ID, PROFILE_ID))
				.thenReturn(MeetingService.ConnectResult.NOT_JOINED);

		handler.afterConnectionEstablished(session);

		verify(session).close(argThat(status -> status.getCode() == 4403));
		verifyNoInteractions(participantRepository, sessionRegistry, sttService);
	}

	@Test
	void registeredParticipantBroadcastsLeftOnClose() {
		attributes.put(MeetingWebSocketHandler.ATTR_JOINED_PARTICIPANT, true);
		when(sessionRegistry.hasSessions(MEETING_ID)).thenReturn(false);

		handler.afterConnectionClosed(session, CloseStatus.NORMAL);

		InOrder order = inOrder(sessionRegistry, sttService);
		order.verify(sessionRegistry).remove(MEETING_ID, session);
		order.verify(sessionRegistry).broadcast(eq(MEETING_ID), any(ParticipantLeftMessage.class));
		order.verify(sessionRegistry).hasSessions(MEETING_ID);
		order.verify(sttService).closeMeeting(MEETING_ID);
		verify(sessionRegistry).broadcast(eq(MEETING_ID), argThat(payload ->
				payload instanceof ParticipantLeftMessage message
						&& PROFILE_ID.equals(message.profileId())));
	}

	@Test
	void rejectedConnectionDoesNotBroadcastLeftOnClose() {
		handler.afterConnectionClosed(session, CloseStatus.NORMAL);

		verify(sessionRegistry, never()).remove(any(), any());
		verify(sessionRegistry, never()).broadcast(any(), any());
		verifyNoInteractions(sttService);
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
