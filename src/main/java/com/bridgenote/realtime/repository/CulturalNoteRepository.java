package com.bridgenote.realtime.repository;

import com.bridgenote.realtime.domain.CulturalNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CulturalNoteRepository extends JpaRepository<CulturalNote, String> {

	/** 회의의 각주들(생성순) — 문화 가이드 탭 재료. */
	List<CulturalNote> findByMeetingIdOrderByCreatedAtAsc(String meetingId);

	boolean existsBySentenceId(String sentenceId);
}
