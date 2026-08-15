package com.bridgenote.realtime.session;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 회의별 "현재 발화자(speaker_index)" 상태. speaker_switch로 갱신되고,
 * 이후 도착하는 audio_chunk가 이 화자로 태깅된다.
 */
@Component
public class MeetingSpeakerState {

	private final Map<String, Integer> currentSpeaker = new ConcurrentHashMap<>();

	public void set(String meetingId, Integer speakerIndex) {
		if (meetingId != null && speakerIndex != null) {
			currentSpeaker.put(meetingId, speakerIndex);
		}
	}

	public Integer get(String meetingId) {
		return currentSpeaker.get(meetingId);
	}

	public void clear(String meetingId) {
		currentSpeaker.remove(meetingId);
	}
}
