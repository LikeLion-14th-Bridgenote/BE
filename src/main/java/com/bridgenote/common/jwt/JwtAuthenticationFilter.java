package com.bridgenote.common.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 요청의 {@code Authorization: Bearer <JWT>}를 검증해 인증 사용자를 request 속성에 저장한다.
 * 토큰이 없거나 유효하지 않으면 인증 없이 통과시키고, 보호가 필요한 엔드포인트는
 * {@link CurrentUserArgumentResolver}가 401로 처리한다. (공개 엔드포인트는 그대로 통과)
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	public static final String AUTH_USER_ATTR = "authUser";
	private static final String BEARER_PREFIX = "Bearer ";

	private final SupabaseJwtProvider jwtProvider;

	public JwtAuthenticationFilter(SupabaseJwtProvider jwtProvider) {
		this.jwtProvider = jwtProvider;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			String token = header.substring(BEARER_PREFIX.length()).trim();
			try {
				AuthUser user = jwtProvider.verify(token);
				request.setAttribute(AUTH_USER_ATTR, user);
			} catch (InvalidTokenException ignored) {
				// 토큰이 있으나 유효하지 않음 → 미인증 상태로 진행. 보호 엔드포인트는 리졸버가 401.
			}
		}
		filterChain.doFilter(request, response);
	}
}
