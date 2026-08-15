package com.bridgenote.realtime.domain;

import com.bridgenote.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 확정 발화의 언어별 번역(AI /ai/analyze translations)을 저장.
 * 회의록 "전체 전사 기록" 탭(원문+번역문)과 과거 회의 번역 재열람의 원천.
 * (sentence_id, lang) 유니크 — 한 발화의 한 언어 번역은 하나.
 */
@Entity
@Table(name = "utterance_translation", uniqueConstraints =
		@UniqueConstraint(name = "uk_utterance_translation_sentence_lang", columnNames = {"sentence_id", "lang"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UtteranceTranslation extends BaseTimeEntity {

	@Id
	@Column(length = 36)
	private String id;

	@Column(name = "meeting_id", length = 36, nullable = false)
	private String meetingId;

	@Column(name = "sentence_id", length = 64, nullable = false)
	private String sentenceId;

	@Column(length = 10, nullable = false)
	private String lang;

	@Column(columnDefinition = "text")
	private String text;

	@Builder
	private UtteranceTranslation(String meetingId, String sentenceId, String lang, String text) {
		this.meetingId = meetingId;
		this.sentenceId = sentenceId;
		this.lang = lang;
		this.text = text;
	}

	@PrePersist
	private void assignId() {
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
	}
}
