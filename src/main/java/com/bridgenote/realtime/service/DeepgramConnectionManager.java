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
	private static final long RECONNECT_COOLDOWN_MS = 3000;   // 연결 실패 후 재시도 최소 간격

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

	/**
	 * 오디오 바이트를 (회의, 언어)별 Deepgram 연결로 전송. 같은 언어 화자는 같은 연결을 공유한다.
	 * 이렇게 하면 한 기기에서 발화자를 전환해도 Deepgram 연결이 유지되어 webm 스트림이 끊기지 않는다.
	 */
	public void sendAudio(String meetingId, Integer speakerIndex, String lang, byte[] audio, TranscriptListener listener) {
		String effLang = (lang != null && !lang.isBlank()) ? lang : language;
		String key = meetingId + "|" + effLang;
		// 없거나 닫힌 연결이면 새로 연다.
		DeepgramLiveConnection conn = connections.compute(key, (k, existing) ->
				(existing == null || existing.reconnectDue(RECONNECT_COOLDOWN_MS))
						? openConnection(meetingId, effLang, speakerIndex, listener) : existing);
		// 기존 연결 재사용 시 현재 화자 인덱스를 갱신 (전사 결과에 최신 화자가 태깅되도록)
		conn.updateSpeakerIndex(speakerIndex);
		conn.sendAudio(audio);
	}

	/** 회의 종료/마지막 참가자 퇴장 시 그 회의의 모든 언어 연결을 닫는다. */
	public void close(String meetingId) {
		String prefix = meetingId + "|";
		connections.entrySet().removeIf(e -> {
			if (e.getKey().startsWith(prefix)) {
				e.getValue().close();
				log.info("Deepgram 연결 종료 {}", e.getKey());
				return true;
			}
			return false;
		});
	}

	private DeepgramLiveConnection openConnection(String meetingId, String lang, Integer speakerIndex, TranscriptListener listener) {
		DeepgramLiveConnection conn = new DeepgramLiveConnection(listener, objectMapper, lang, speakerIndex);
		if (apiKey.isBlank()) {
			log.warn("DEEPGRAM_API_KEY 미설정 — STT 비활성 (meeting={})", meetingId);
			conn.markOpenFailed();   // 재시도해도 계속 실패 → 매 청크 새 객체 생성 억제
			return conn; // ws 없음 → sendAudio no-op
		}
		try {
			HttpClient.newHttpClient()
					.newWebSocketBuilder()
					.header("Authorization", "Token " + apiKey)
					.buildAsync(URI.create(buildUrl(lang)), conn)
					.get(5, TimeUnit.SECONDS);
			log.info("Deepgram 연결됨 meeting={} model={} language={}", meetingId, model, lang);
		} catch (Exception e) {
			log.error("Deepgram 연결 실패 meeting={} lang={}: {}", meetingId, lang, e.getMessage());
			conn.markOpenFailed();   // 실패 표시 → 다음 청크에서 즉시 재시도 안 하고 cooldown 대기
		}
		return conn;
	}

	private String buildUrl(String lang) {
		StringBuilder sb = new StringBuilder(DEEPGRAM_URL)
				.append("?interim_results=true&punctuate=true&smart_format=true");
		if (!model.isBlank()) {
			sb.append("&model=").append(model);
		}
		if (lang != null && !lang.isBlank()) {
			sb.append("&language=").append(lang);
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
