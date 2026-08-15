package com.bridgenote.realtime.repository;

import com.bridgenote.realtime.domain.UtteranceTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UtteranceTranslationRepository extends JpaRepository<UtteranceTranslation, String> {

	/** 전사 페이지의 발화들에 대한 번역 일괄 조회. */
	List<UtteranceTranslation> findBySentenceIdIn(Collection<String> sentenceIds);

	boolean existsBySentenceIdAndLang(String sentenceId, String lang);
}
