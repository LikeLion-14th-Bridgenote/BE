package com.bridgenote.realtime.config;

import com.bridgenote.realtime.handler.AuthHandshakeInterceptor;
import com.bridgenote.realtime.handler.MeetingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 실시간 채널(raw WebSocket) 설정. 엔드포인트: {@code /ws/meetings/{id}}
 * (STT 오디오·자막·번역·경고를 JSON 메시지의 type으로 구분해 주고받는다.)
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class RealtimeWebSocketConfig implements WebSocketConfigurer {

	private final MeetingWebSocketHandler meetingWebSocketHandler;
	private final AuthHandshakeInterceptor authHandshakeInterceptor;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(meetingWebSocketHandler, "/ws/meetings/*")
				.addInterceptors(authHandshakeInterceptor)
				.setAllowedOriginPatterns("*");
	}
}
