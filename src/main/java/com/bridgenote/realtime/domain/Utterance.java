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

import java.time.Instant;
import java.util.UUID;

/**
 * 확정 발화(STT is_final). 회의록 생성의 재료가 된다. PK는 UUID(VARCHAR(36)).
 *
 * <p>ERD는 participant_id(FK)를 두지만, 실시간은 speaker_index로 동작하므로 둘 다 보관한다.
 * (participant_id는 meeting_id+speaker_index로 추후 매핑 가능 — 지금은 nullable)
 */
@Entity
@Table(name = "utterance")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Utterance extends BaseTimeEntity {

	@Id
	@Column(length = 36)
	private String id;

	@Column(name = "sentence_id", length = 64)
	private String sentenceId;

	@Column(name = "meeting_id", length = 36, nullable = false)
	private String meetingId;

	@Column(name = "participant_id", length = 36)
	private String participantId;

	@Column(name = "speaker_index")
	private Integer speakerIndex;

	@Column(name = "source_lang", length = 10)
	private String sourceLang;

	@Column(name = "source_text", columnDefinition = "text")
	private String sourceText;

	@Column(name = "is_final", nullable = false)
	private boolean isFinal;

	@Column(name = "spoken_at")
	private Instant spokenAt;

	@Builder
	private Utterance(String sentenceId, String meetingId, String participantId, Integer speakerIndex,
					  String sourceLang, String sourceText, boolean isFinal, Instant spokenAt) {
		this.sentenceId = sentenceId;
		this.meetingId = meetingId;
		this.participantId = participantId;
		this.speakerIndex = speakerIndex;
		this.sourceLang = sourceLang;
		this.sourceText = sourceText;
		this.isFinal = isFinal;
		this.spokenAt = spokenAt;
	}

	@PrePersist
	private void assignId() {
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
	}
}
