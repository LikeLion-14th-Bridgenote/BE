package com.bridgenote.common;

import com.bridgenote.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 초기 세팅 확인용 헬스체크. Actuator /actuator/health 와 별개로 표준 응답 형식을 확인한다.
 */
@RestController
public class HealthController {

	@GetMapping("/ping")
	public ApiResponse<Map<String, String>> ping() {
		return ApiResponse.success(Map.of("status", "ok", "service", "bridgenote"));
	}
}
