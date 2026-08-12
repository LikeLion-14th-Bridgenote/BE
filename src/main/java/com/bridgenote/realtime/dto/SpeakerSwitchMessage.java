package com.bridgenote.realtime.dto;

/**
 * 클라이언트 → 서버: 발화자 전환. 이후 audio_chunk가 이 화자 번호로 태깅된다.
 * (JSON snake_case: speaker_index)
 */
public record SpeakerSwitchMessage(
		String type,
		Integer speakerIndex
) {
}
