package com.bridgenote.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 설정. {@link EnableJpaAuditing}으로 BaseTimeEntity의 created_at/updated_at 자동 기록 활성화.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
