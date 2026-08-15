package com.bridgenote.participant.domain;

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

import java.time.Instant;
import java.util.UUID;

/**
 * 회의 참가자. (meeting_id, profile_id) 유니크 — 한 사용자는 한 회의에 하나의 행.
 * 생명주기: consent(동의) → join(입장 시 화자번호 배정).
 * profile_id = 로그인 사용자 id (Supabase sub). PK는 UUID(VARCHAR(36)).
 *
 * <p>nickname·language는 profile(수민 User) 값이지만 현재 id 타입(Long↔UUID) 불일치로
 * join 시점 스냅샷으로 보관한다. (id 정렬 후 profile 조인으로 전환 가능)
 */
@Entity
@Table(name = "participant", uniqueConstraints =
		@UniqueConstraint(name = "uk_participant_meeting_profile", columnNames = {"meeting_id", "profile_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participant extends BaseTimeEntity {

	@Id
	@Column(length = 36)
	private String id;

	@Column(name = "meeting_id", length = 36, nullable = false)
	private String meetingId;

	@Column(name = "profile_id", length = 36, nullable = false)
	private String profileId;

	/** 표시 이름(입장 시점 스냅샷). */
	@Column(length = 100)
	private String nickname;

	/** 번역 타겟 언어(입장 시점 스냅샷). 예: ko, vi, en */
	@Column(length = 10)
	private String language;

	/** 화자 번호 — 입장(join) 시 배정. null이면 아직 입장 전. */
	@Column(name = "speaker_index")
	private Integer speakerIndex;

	/** 데이터 처리 동의 여부. */
	@Column(nullable = false)
	private boolean consented;

	@Column(name = "consented_at")
	private Instant consentedAt;

	@Column(name = "joined_at")
	private Instant joinedAt;

	@Builder
	private Participant(String meetingId, String profileId) {
		this.meetingId = meetingId;
		this.profileId = profileId;
	}

	@PrePersist
	private void assignId() {
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
	}

	/** 데이터 처리 동의 처리. 미동의면 consentedAt은 비운다. */
	public void applyConsent(boolean agreed, Instant at) {
		this.consented = agreed;
		this.consentedAt = agreed ? at : null;
	}

	/** 입장 완료 — 화자번호·프로필 스냅샷·입장시각 기록. */
	public void completeJoin(String nickname, String language, int speakerIndex, Instant joinedAt) {
		this.nickname = nickname;
		this.language = language;
		this.speakerIndex = speakerIndex;
		this.joinedAt = joinedAt;
	}

	/** 입장(화자번호 배정) 완료 여부. */
	public boolean isJoined() {
		return this.speakerIndex != null;
	}
}
