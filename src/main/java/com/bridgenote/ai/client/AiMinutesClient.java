package com.bridgenote.ai.client;

import com.bridgenote.ai.dto.MinutesRequest;
import com.bridgenote.ai.dto.MinutesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * bridgenote-AI(FastAPI) 회의록 생성 호출. 회의 종료 후 배치라 LLM 처리에 수 초 걸릴 수 있어
 * <b>비동기</b>로 호출하고 결과/실패를 콜백으로 넘긴다(회의 종료 응답을 막지 않는다).
 *
 * <p>직렬화/역직렬화는 앱 전역 {@code tools.jackson}(snake_case) ObjectMapper로 직접 처리한다.
 */
@Slf4j
@Component
public class AiMinutesClient {

	// 회의록은 발화 전체 배치 LLM 처리라 analyze보다 넉넉한 타임아웃.
	private static final Duration TIMEOUT = Duration.ofSeconds(60);

	private final WebClient webClient;
	private final ObjectMapper objectMapper;

	public AiMinutesClient(ObjectMapper objectMapper,
						   @Value("${bridgenote.ai.server-url}") String serverUrl) {
		// WebClient.builder() 정적 팩토리 사용(주입식 WebClient.Builder 빈은 이 webmvc 앱에 없음).
		this.webClient = WebClient.builder().baseUrl(serverUrl).build();
		this.objectMapper = objectMapper;
	}

	/** /ai/minutes 비동기 호출. 성공 시 {@code onResult}, 실패 시 {@code onError}. */
	public void generate(MinutesRequest request,
						 Consumer<MinutesResponse> onResult, Consumer<Throwable> onError) {
		final String body;
		try {
			body = objectMapper.writeValueAsString(request); // Jackson 3: unchecked
		} catch (RuntimeException e) {
			log.warn("/ai/minutes 요청 직렬화 실패 meeting={}", request.meetingId(), e);
			onError.accept(e);
			return;
		}

		webClient.post()
				.uri("/ai/minutes")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(body)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(TIMEOUT)
				.map(json -> objectMapper.readValue(json, MinutesResponse.class)) // Jackson 3: unchecked
				.subscribe(onResult, onError);
	}
}
