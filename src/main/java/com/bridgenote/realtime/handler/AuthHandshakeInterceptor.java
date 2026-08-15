package com.bridgenote.realtime.handler;

import com.bridgenote.common.jwt.AuthUser;
import com.bridgenote.common.jwt.SupabaseJwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * WS 핸드셰이크 시 meetingId(경로)와 token(쿼리 파라미터)을 세션 속성에 저장한다.
 * 실제 인증·회의존재 검증과 종료코드(4401/4404)는 {@link MeetingWebSocketHandler}에서 처리한다.
 * (핸드셰이크 단계에서 거부하면 WS close code를 못 주기 때문)
 */
@Component
@RequiredArgsConstructor
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

	public static final String ATTR_AUTH_USER = "authUser";
	public static final String ATTR_MEETING_ID = "meetingId";

	private final SupabaseJwtProvider jwtProvider;

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
								   WebSocketHandler wsHandler, Map<String, Object> attributes) {
		URI uri = request.getURI();

		// 경로 마지막 세그먼트 = meetingId (/ws/meetings/{id})
		String path = uri.getPath();
		String meetingId = path.substring(path.lastIndexOf('/') + 1);
		attributes.put(ATTR_MEETING_ID, meetingId);

		// 쿼리 파라미터 token 검증(있을 때). 실패/부재면 authUser 미설정 → 핸들러가 4401로 종료.
		String token = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
		if (token != null && !token.isBlank()) {
			try {
				AuthUser user = jwtProvider.verify(token);
				attributes.put(ATTR_AUTH_USER, user);
			} catch (Exception ignored) {
				// 유효하지 않은 토큰 → authUser 없음
			}
		}
		return true; // 핸드셰이크는 수락, 인증 실패 종료코드는 핸들러에서
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
							   WebSocketHandler wsHandler, Exception exception) {
		// no-op
	}
}
