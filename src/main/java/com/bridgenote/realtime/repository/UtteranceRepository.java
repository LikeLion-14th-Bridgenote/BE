package com.bridgenote.realtime.repository;

import com.bridgenote.realtime.domain.Utterance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UtteranceRepository extends JpaRepository<Utterance, String> {

	/** 회의의 확정 발화들(시간순) — 회의록 생성 재료. */
	List<Utterance> findByMeetingIdOrderBySpokenAtAsc(String meetingId);
}
