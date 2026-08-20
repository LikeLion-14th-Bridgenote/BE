package com.bridgenote.realtime.service;

/**
 * Deepgram 전사 결과 콜백. (meetingId는 연결마다 고정이라 별도 전달)
 * speakerIndex는 연결 생성 시 확정된 화자 번호를 그대로 전달한다(전역 상태 의존 제거).
 */
@FunctionalInterface
public interface TranscriptListener {

	void onTranscript(String sentenceId, String sourceLang, String text, boolean isFinal, Integer speakerIndex);
}
