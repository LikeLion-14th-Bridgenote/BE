package com.bridgenote.realtime.service;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Deepgram 스트리밍 STT 연결 하나(회의 1개). {@link WebSocket.Listener}로 전사 이벤트를 받아
 * {@link TranscriptListener}로 넘긴다. 오디오 전송은 순서 보장을 위해 직렬화한다.
 */
@Slf4j
public class DeepgramLiveConnection implements WebSocket.Listener {

	private final TranscriptListener transcriptListener;
	private final ObjectMapper objectMapper;
	private final String language;

	// sentence_id는 전역 유일해야 한다. 예전 "s-{counter}"는 연결마다 0부터 시작해서
	// 회의 간(그리고 언어별 연결 간) s-0이 겹쳤고, 저장 중복검사에 걸려 각주·번역이 조용히 유실됐다.
	private volatile String currentSentenceId = UUID.randomUUID().toString();
	private final StringBuilder buffer = new StringBuilder();

	private volatile WebSocket ws;
	private volatile boolean closed = false;   // 유휴/오류로 닫힌 연결 표시 → 매니저가 재연결
	private final Object sendLock = new Object();
	private CompletableFuture<WebSocket> sendChain = CompletableFuture.completedFuture(null);

	public DeepgramLiveConnection(TranscriptListener transcriptListener, ObjectMapper objectMapper, String language) {
		this.transcriptListener = transcriptListener;
		this.objectMapper = objectMapper;
		this.language = language;
	}

	// ===== WebSocket.Listener =====

	@Override
	public void onOpen(WebSocket webSocket) {
		this.ws = webSocket;
		webSocket.request(1);
	}

	@Override
	public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		buffer.append(data);
		if (last) {
			String json = buffer.toString();
			buffer.setLength(0);
			handleTranscript(json);
		}
		webSocket.request(1);
		return null;
	}

	@Override
	public void onError(WebSocket webSocket, Throwable error) {
		log.warn("Deepgram WS 오류: {}", error.getMessage());
		markClosed();   // 오류 난 연결은 재사용 불가 → 재연결 대상
	}

	@Override
	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		log.info("Deepgram WS 종료 code={} reason={}", statusCode, reason);
		markClosed();   // 유휴 타임아웃 등으로 닫힘 → 다음 오디오 때 매니저가 새로 연다
		return null;
	}

	/** 유휴/오류로 닫힌 연결인지. 매니저가 이걸 보고 새 연결로 교체한다. */
	public boolean isClosed() {
		return closed || ws == null;
	}

	private void markClosed() {
		this.closed = true;
		this.ws = null;   // sendAudio가 죽은 ws로 계속 쏴서 sendChain이 영구 실패하는 것 방지
	}

	// ===== 전송/종료 =====

	/** 오디오 바이트 전송(직렬화 — 이전 전송 완료 후 다음 전송). */
	public void sendAudio(byte[] audio) {
		WebSocket w = this.ws;
		if (w == null || audio == null || audio.length == 0) {
			return;
		}
		synchronized (sendLock) {
			sendChain = sendChain.thenCompose(x -> w.sendBinary(ByteBuffer.wrap(audio), true));
		}
	}

	/** 스트림 종료 신호 후 연결 닫기. */
	public void close() {
		WebSocket w = this.ws;
		if (w == null) {
			return;
		}
		try {
			w.sendText("{\"type\":\"CloseStream\"}", true);
			w.sendClose(WebSocket.NORMAL_CLOSURE, "closed");
		} catch (Exception e) {
			log.debug("Deepgram close 중 오류: {}", e.getMessage());
		}
	}

	// ===== 전사 파싱 =====

	private void handleTranscript(String json) {
		try {
			DeepgramResult result = objectMapper.readValue(json, DeepgramResult.class);
			if (result.channel() == null
					|| result.channel().alternatives() == null
					|| result.channel().alternatives().isEmpty()) {
				return;
			}
			String text = result.channel().alternatives().get(0).transcript();
			if (text == null || text.isBlank()) {
				return;
			}
			boolean isFinal = Boolean.TRUE.equals(result.isFinal());
			String sentenceId = currentSentenceId;
			transcriptListener.onTranscript(sentenceId, language, text, isFinal);
			if (isFinal) {
				currentSentenceId = UUID.randomUUID().toString(); // 다음 발화용 새(유일) sentence_id
			}
		} catch (Exception e) {
			log.debug("Deepgram 응답 파싱 스킵: {}", e.getMessage());
		}
	}

	// Deepgram 응답(필요한 필드만; 나머지는 무시). is_final ↔ isFinal (전역 snake_case)
	private record DeepgramResult(String type, Boolean isFinal, DeepgramChannel channel) {
	}

	private record DeepgramChannel(List<DeepgramAlternative> alternatives) {
	}

	private record DeepgramAlternative(String transcript) {
	}
}
