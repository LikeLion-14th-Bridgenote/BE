package com.bridgenote.realtime.dto;

/**
 * 클라이언트 → 서버: 오디오 청크(base64). 서버는 STT로 넘긴다. 별도 응답 없음(결과는 caption 브로드캐스트).
 * (JSON snake_case: speaker_index)
 */
public record AudioChunkMessage(
		String type,
		Integer speakerIndex,
		Long seq,
		String data
) {
}
