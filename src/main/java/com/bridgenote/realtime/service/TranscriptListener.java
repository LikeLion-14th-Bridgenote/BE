package com.bridgenote.realtime.service;

/**
 * Deepgram 전사 결과 콜백. (meetingId는 연결마다 고정이라 별도 전달)
 * 실제 화자(speaker_index)는 수신 시점의 현재 화자 상태로 해석하므로 여기선 전달하지 않는다.
 */
@FunctionalInterface
public interface TranscriptListener {

	void onTranscript(String sentenceId, String sourceLang, String text, boolean isFinal);
}
