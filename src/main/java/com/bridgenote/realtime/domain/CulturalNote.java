package com.bridgenote.realtime.domain;

import com.bridgenote.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 문화 각주(AI /ai/analyze의 has_risk=true 결과)를 회의별로 저장.
 * 회의록 "문화 가이드" 탭(note_type별 집계)과 과거 회의 각주 재열람의 원천.
 * cultural_note ERD와 정합: risk_level·note_type·speaker_intent·listener_misread·advice·rewrite_text.
 */
@Entity
@Table(name = "cultural_note")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CulturalNote extends BaseTimeEntity {

	@Id
	@Column(length = 36)
	private String id;

	@Column(name = "meeting_id", length = 36, nullable = false)
	private String meetingId;

	@Column(name = "sentence_id", length = 64)
	private String sentenceId;

	/** 각주 대상 발화의 화자 번호(표시용). */
	@Column(name = "speaker_index")
	private Integer speakerIndex;

	@Column(name = "risk_level", length = 10)
	private String riskLevel;

	/** 프론트 3종 라벨(문화 이해 / 커뮤니케이션 / 업무 스타일). 문화 가이드 탭 집계 기준. */
	@Column(name = "note_type", length = 50)
	private String noteType;

	@Column(name = "speaker_intent", columnDefinition = "text")
	private String speakerIntent;

	@Column(name = "listener_misread", columnDefinition = "text")
	private String listenerMisread;

	@Column(columnDefinition = "text")
	private String advice;

	@Column(name = "rewrite_text", columnDefinition = "text")
	private String rewriteText;

	@Builder
	private CulturalNote(String meetingId, String sentenceId, Integer speakerIndex, String riskLevel,
						 String noteType, String speakerIntent, String listenerMisread,
						 String advice, String rewriteText) {
		this.meetingId = meetingId;
		this.sentenceId = sentenceId;
		this.speakerIndex = speakerIndex;
		this.riskLevel = riskLevel;
		this.noteType = noteType;
		this.speakerIntent = speakerIntent;
		this.listenerMisread = listenerMisread;
		this.advice = advice;
		this.rewriteText = rewriteText;
	}

	@PrePersist
	private void assignId() {
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
	}
}
