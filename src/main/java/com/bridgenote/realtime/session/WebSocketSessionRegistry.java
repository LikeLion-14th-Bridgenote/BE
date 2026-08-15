package com.bridgenote.realtime.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 회의별 WebSocket 세션 레지스트리 + 브로드캐스트.
 * STT/AI 결과(자막·번역·경고)를 해당 회의 참가자 전원에게 push할 때 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionRegistry {

	// Spring Boot 4 = Jackson 3 (tools.jackson). 전역 snake_case 설정이 적용된 매퍼.
	private final ObjectMapper objectMapper;
	private final Map<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

	public void add(String meetingId, WebSocketSession session) {
		sessions.computeIfAbsent(meetingId, k -> ConcurrentHashMap.newKeySet()).add(session);
	}

	public void remove(String meetingId, WebSocketSession session) {
		Set<WebSocketSession> set = sessions.get(meetingId);
		if (set != null) {
			set.remove(session);
			if (set.isEmpty()) {
				sessions.remove(meetingId);
			}
		}
	}

	/** 해당 회의에 열린 세션이 남아있는지. */
	public boolean hasSessions(String meetingId) {
		Set<WebSocketSession> set = sessions.get(meetingId);
		return set != null && !set.isEmpty();
	}

	/** 회의 참가자 전원에게 JSON 메시지를 전송한다. */
	public void broadcast(String meetingId, Object payload) {
		Set<WebSocketSession> set = sessions.get(meetingId);
		if (set == null || set.isEmpty()) {
			return;
		}

		String json;
		try {
			json = objectMapper.writeValueAsString(payload); // Jackson 3: unchecked
		} catch (RuntimeException e) {
			log.error("WS 메시지 직렬화 실패", e);
			return;
		}

		TextMessage message = new TextMessage(json);
		for (WebSocketSession session : set) {
			try {
				// WebSocketSession.sendMessage는 thread-safe하지 않음. 여러 스레드가 같은 세션에
				// 동시에 쓰면 TEXT_PARTIAL_WRITING 상태 깨짐(→연결 1011 종료). 세션별로 직렬화한다.
				synchronized (session) {
					if (session.isOpen()) {
						session.sendMessage(message);
					}
				}
			} catch (Exception e) {
				// 한 세션 전송 실패가 다른 세션/브로드캐스트 전체를 막지 않도록 방어.
				log.warn("WS 전송 실패 session={}: {}", session.getId(), e.toString());
			}
		}
	}
}
