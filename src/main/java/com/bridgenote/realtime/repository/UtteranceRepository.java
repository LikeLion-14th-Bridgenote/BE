package com.bridgenote.realtime.repository;

import com.bridgenote.realtime.domain.Utterance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UtteranceRepository extends JpaRepository<Utterance, String> {

	/** 회의의 확정 발화들(시간순) — 회의록 생성 재료. */
	List<Utterance> findByMeetingIdOrderBySpokenAtAsc(String meetingId);

	/** 회의의 확정 발화 페이지(시간순) — 전체 전사 기록 탭. */
	Page<Utterance> findByMeetingIdOrderBySpokenAtAsc(String meetingId, Pageable pageable);
}
