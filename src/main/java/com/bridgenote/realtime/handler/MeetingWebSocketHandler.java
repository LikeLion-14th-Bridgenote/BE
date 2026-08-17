package com.bridgenote.realtime.handler;

import com.bridgenote.common.jwt.AuthUser;
import com.bridgenote.meeting.service.MeetingService;
import com.bridgenote.participant.repository.ParticipantRepository;
import com.bridgenote.realtime.dto.AudioChunkMessage;
import com.bridgenote.realtime.dto.MeetingStartedMessage;
import com.bridgenote.realtime.dto.ParticipantJoinedMessage;
import com.bridgenote.realtime.dto.ParticipantLeftMessage;
import com.bridgenote.realtime.dto.SpeakerSwitchMessage;
import com.bridgenote.realtime.service.SttService;
import com.bridgenote.realtime.session.MeetingSpeakerState;
import com.bridgenote.realtime.session.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

/**
 * 회의방 실시간 채널 핸들러.
 * 연결 시 토큰/회의 상태를 검증한다(4401 인증 실패 / 4404 회의 없음 / 4409 종료된 회의).
 * 첫 참가자 접속 시 회의를 WAITING→LIVE로 전환하고, 통과하면 세션을 등록한다.
 * 연결 후에는 메시지 {@code type}으로 구분한다: speaker_switch(발화자 전환) / audio_chunk(오디오→STT).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingWebSocketHandler extends TextWebSocketHandler {
	static final String ATTR_JOINED_PARTICIPANT = "joinedParticipant";

	/** 인증 실패(토큰 없음/만료) */
	private static final CloseStatus UNAUTHORIZED = new CloseStatus(4401, "인증 실패");
	/** consent → join을 완료하지 않음 */
	private static final CloseStatus JOIN_REQUIRED = new CloseStatus(4403, "회의 참가 필요");
	/** 회의 없음 */
	private static final CloseStatus MEETING_NOT_FOUND = new CloseStatus(4404, "회의 없음");
	/** 이미 종료된 회의 */
	private static final CloseStatus MEETING_ENDED = new CloseStatus(4409, "회의 종료됨");

	private final MeetingService meetingService;
	private final ParticipantRepository participantRepository;
	private final WebSocketSessionRegistry sessionRegistry;
	private final MeetingSpeakerState speakerState;
	private final SttService sttService;
	private final ObjectMapper objectMapper;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		AuthUser user = (AuthUser) session.getAttributes().get(AuthHandshakeInterceptor.ATTR_AUTH_USER);
		String meetingId = (String) session.getAttributes().get(AuthHandshakeInterceptor.ATTR_MEETING_ID);

		if (user == null) {
			session.close(UNAUTHORIZED);
			return;
		}
		if (meetingId == null || meetingId.isBlank()) {
			session.close(MEETING_NOT_FOUND);
			return;
		}

		// join 완료 여부 + 회의 상태 판정. 검증을 통과한 첫 접속만 WAITING→LIVE로 전환한다.
		MeetingService.ConnectResult result = meetingService.onParticipantConnect(meetingId, user.id());
		switch (result) {
			case NOT_FOUND -> { session.close(MEETING_NOT_FOUND); return; }
			case ENDED -> { session.close(MEETING_ENDED); return; }
			case NOT_JOINED -> { session.close(JOIN_REQUIRED); return; }
			default -> { /* LIVE / STARTED → 진행 */ }
		}

		// 서비스 검증 직후 참가자가 사라지는 예외 상황에서도 미등록 세션을 방에 넣지 않는다.
		var participant = participantRepository.findByMeetingIdAndProfileId(meetingId, user.id())
				.filter(p -> p.isJoined())
				.orElse(null);
		if (participant == null) {
			session.close(JOIN_REQUIRED);
			return;
		}

		// 먼저 등록해야 새 참가자 본인까지 participant_joined를 수신한다.
		sessionRegistry.add(meetingId, session);
		session.getAttributes().put(ATTR_JOINED_PARTICIPANT, true);
		sessionRegistry.broadcast(meetingId, ParticipantJoinedMessage.of(participant));

		// 첫 참가자 접속으로 회의가 시작됐으면 본인 포함 전원에게 상태 전환 알림
		if (result == MeetingService.ConnectResult.STARTED) {
			sessionRegistry.broadcast(meetingId, MeetingStartedMessage.of(meetingId, Instant.now()));
		}
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		String meetingId = (String) session.getAttributes().get(AuthHandshakeInterceptor.ATTR_MEETING_ID);
		String payload = message.getPayload();
		try {
			Map<?, ?> raw = objectMapper.readValue(payload, Map.class);
			String type = String.valueOf(raw.get("type"));
			switch (type) {
				case "speaker_switch" -> {
					SpeakerSwitchMessage m = objectMapper.readValue(payload, SpeakerSwitchMessage.class);
					speakerState.set(meetingId, m.speakerIndex());
				}
				case "audio_chunk" -> {
					AudioChunkMessage m = objectMapper.readValue(payload, AudioChunkMessage.class);
					// 명시된 speaker_index 우선, 없으면 speaker_switch로 설정된 현재 화자
					Integer speaker = (m.speakerIndex() != null) ? m.speakerIndex() : speakerState.get(meetingId);
					sttService.submitAudio(meetingId, speaker, m.seq(), m.data());
				}
				default -> log.debug("알 수 없는 WS 메시지 type={}", type);
			}
		} catch (Exception e) {
			log.warn("WS 메시지 처리 실패: {}", e.getMessage());
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		AuthUser user = (AuthUser) session.getAttributes().get(AuthHandshakeInterceptor.ATTR_AUTH_USER);
		String meetingId = (String) session.getAttributes().get(AuthHandshakeInterceptor.ATTR_MEETING_ID);
		boolean joinedParticipant = Boolean.TRUE.equals(session.getAttributes().get(ATTR_JOINED_PARTICIPANT));
		if (meetingId != null && joinedParticipant) {
			sessionRegistry.remove(meetingId, session);
			// 남은 참가자들에게 퇴장 알림 (본인 세션은 이미 제거됨)
			if (user != null) {
				sessionRegistry.broadcast(meetingId, ParticipantLeftMessage.of(user.id()));
			}
			if (!sessionRegistry.hasSessions(meetingId)) {
				sttService.closeMeeting(meetingId); // 마지막 참가자 퇴장 → Deepgram 연결/화자상태 정리
			}
		}
	}
}
