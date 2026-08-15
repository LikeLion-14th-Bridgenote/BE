package com.bridgenote.realtime.config;

import com.bridgenote.realtime.handler.AuthHandshakeInterceptor;
import com.bridgenote.realtime.handler.MeetingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

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

	/**
	 * WS 메시지 버퍼 상향. 기본 8KB로는 audio_chunk(base64 오디오)가 넘쳐 Tomcat이
	 * handleTextMessage 호출 전에 세션을 끊어버린다(자막이 안 뜨는 원인). 1MB로 여유 확보.
	 */
	@Bean
	public ServletServerContainerFactoryBean createWebSocketContainer() {
		ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
		container.setMaxTextMessageBufferSize(1024 * 1024);
		container.setMaxBinaryMessageBufferSize(1024 * 1024);
		return container;
	}
}
