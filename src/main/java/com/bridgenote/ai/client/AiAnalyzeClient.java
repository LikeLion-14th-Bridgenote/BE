package com.bridgenote.ai.client;

import com.bridgenote.ai.dto.AnalyzeRequest;
import com.bridgenote.ai.dto.AnalyzeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * bridgenote-AI(FastAPI) 호출 클라이언트. 실시간 경로라 <b>비동기</b>로 호출하고,
 * 응답을 콜백으로 넘긴다(STT 스레드를 막지 않는다). 실패(AI 다운/타임아웃/계약오류)는
 * 로그만 남기고 조용히 스킵 — 자막·번역 파이프라인 전체가 죽지 않도록.
 *
 * <p>직렬화/역직렬화는 앱 전역 {@code tools.jackson}(snake_case) ObjectMapper로 직접 처리한다.
 * WebClient 코덱 설정에 의존하지 않아 요청은 항상 sentence_id/source_text/job 형태로 나간다.
 */
@Slf4j
@Component
public class AiAnalyzeClient {

	private static final Duration TIMEOUT = Duration.ofSeconds(8);

	private final WebClient webClient;
	// Spring Boot 4 = Jackson 3(tools.jackson). WS 브로드캐스트와 동일한 전역 snake_case 매퍼.
	private final ObjectMapper objectMapper;

	public AiAnalyzeClient(ObjectMapper objectMapper,
						   @Value("${bridgenote.ai.server-url}") String serverUrl) {
		// WebClient.builder() 정적 팩토리 사용(주입식 WebClient.Builder 빈은 이 webmvc 앱에 없음).
		// 직렬화/역직렬화는 objectMapper로 직접 하므로 WebClient 코덱 설정은 불필요.
		this.webClient = WebClient.builder().baseUrl(serverUrl).build();
		this.objectMapper = objectMapper;
	}

	/** /ai/analyze 비동기 호출. 성공 시 {@code onResult}로 응답 전달, 실패 시 로그만. */
	public void analyze(AnalyzeRequest request, Consumer<AnalyzeResponse> onResult) {
		final String body;
		try {
			body = objectMapper.writeValueAsString(request); // Jackson 3: unchecked
		} catch (RuntimeException e) {
			log.warn("/ai/analyze 요청 직렬화 실패 sentence={}", request.sentenceId(), e);
			return;
		}

		webClient.post()
				.uri("/ai/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(body)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(TIMEOUT)
				.map(json -> objectMapper.readValue(json, AnalyzeResponse.class)) // Jackson 3: unchecked
				.subscribe(
						onResult,
						err -> log.warn("/ai/analyze 호출 실패 sentence={} meeting={}: {}",
								request.sentenceId(), request.meetingId(), err.toString()));
	}
}
