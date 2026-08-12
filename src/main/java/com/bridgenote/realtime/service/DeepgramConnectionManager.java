package com.bridgenote.realtime.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 회의별 Deepgram 스트리밍 STT 연결 관리 (JDK 내장 WebSocket 사용).
 * 오디오는 {@code wss://api.deepgram.com/v1/listen}로 스트리밍하고, 전사 이벤트는
 * {@link DeepgramLiveConnection}이 {@link TranscriptListener}로 넘긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeepgramConnectionManager {

	private static final String DEEPGRAM_URL = "wss://api.deepgram.com/v1/listen";

	@Value("${deepgram.api-key:}")
	private String apiKey;
	@Value("${deepgram.model:nova-2}")
	private String model;
	@Value("${deepgram.language:multi}")
	private String language;
	@Value("${deepgram.encoding:}")
	private String encoding;
	@Value("${deepgram.sample-rate:}")
	private String sampleRate;

	private final ObjectMapper objectMapper;
	private final Map<String, DeepgramLiveConnection> connections = new ConcurrentHashMap<>();

	/** 오디오 바이트를 해당 회의의 Deepgram 연결로 전송(없으면 연결 생성). */
	public void sendAudio(String meetingId, byte[] audio, TranscriptListener listener) {
		connections.computeIfAbsent(meetingId, id -> openConnection(id, listener)).sendAudio(audio);
	}

	/** 회의 종료/마지막 참가자 퇴장 시 연결 닫기. */
	public void close(String meetingId) {
		DeepgramLiveConnection conn = connections.remove(meetingId);
		if (conn != null) {
			conn.close();
			log.info("Deepgram 연결 종료 meeting={}", meetingId);
		}
	}

	private DeepgramLiveConnection openConnection(String meetingId, TranscriptListener listener) {
		DeepgramLiveConnection conn = new DeepgramLiveConnection(listener, objectMapper, language);
		if (apiKey.isBlank()) {
			log.warn("DEEPGRAM_API_KEY 미설정 — STT 비활성 (meeting={})", meetingId);
			return conn; // ws 없음 → sendAudio no-op
		}
		try {
			HttpClient.newHttpClient()
					.newWebSocketBuilder()
					.header("Authorization", "Token " + apiKey)
					.buildAsync(URI.create(buildUrl()), conn)
					.get(5, TimeUnit.SECONDS);
			log.info("Deepgram 연결됨 meeting={} model={} language={}", meetingId, model, language);
		} catch (Exception e) {
			log.error("Deepgram 연결 실패 meeting={}: {}", meetingId, e.getMessage());
		}
		return conn;
	}

	private String buildUrl() {
		StringBuilder sb = new StringBuilder(DEEPGRAM_URL)
				.append("?interim_results=true&punctuate=true&smart_format=true");
		if (!model.isBlank()) {
			sb.append("&model=").append(model);
		}
		if (!language.isBlank()) {
			sb.append("&language=").append(language);
		}
		// raw 오디오(linear16, mulaw 등)면 encoding+sample_rate 필수. 컨테이너(webm/ogg)면 비워둠(자동감지).
		if (!encoding.isBlank()) {
			sb.append("&encoding=").append(encoding);
		}
		if (!sampleRate.isBlank()) {
			sb.append("&sample_rate=").append(sampleRate);
		}
		return sb.toString();
	}
}
