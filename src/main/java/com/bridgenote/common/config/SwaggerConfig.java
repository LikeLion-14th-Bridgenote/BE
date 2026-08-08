package com.bridgenote.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc(OpenAPI) 설정. Swagger UI: /swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI bridgenoteOpenAPI() {
		return new OpenAPI().info(new Info()
				.title("Bridgenote API")
				.description("Bridgenote 메인 백엔드 API 문서 (인증·프로필·회의·실시간)")
				.version("v0.0.1"));
	}
}
