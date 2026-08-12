package com.bridgenote.common.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link CurrentUser} 파라미터를 request 속성의 {@link AuthUser}로 채운다.
 * 인증 정보가 없으면 401({@link InvalidTokenException}).
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentUser.class)
				&& parameter.getParameterType().equals(AuthUser.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		Object user = (request != null) ? request.getAttribute(JwtAuthenticationFilter.AUTH_USER_ATTR) : null;
		if (user instanceof AuthUser authUser) {
			return authUser;
		}
		throw new InvalidTokenException("인증되지 않은 사용자입니다. 요청을 처리하려면 로그인 후 시도해주세요.");
	}
}
