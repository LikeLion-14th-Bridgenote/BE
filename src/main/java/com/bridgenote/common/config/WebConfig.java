package com.bridgenote.common.config;

import com.bridgenote.common.jwt.CurrentUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC 설정 — {@code @CurrentUser} 아규먼트 리졸버 등록.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final CurrentUserArgumentResolver currentUserArgumentResolver;

	public WebConfig(CurrentUserArgumentResolver currentUserArgumentResolver) {
		this.currentUserArgumentResolver = currentUserArgumentResolver;
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(currentUserArgumentResolver);
	}
}
