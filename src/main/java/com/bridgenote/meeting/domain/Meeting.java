package com.bridgenote.meeting.domain;

import com.bridgenote.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 회의(meeting). PK는 UUID(VARCHAR(36)).
 */
@Entity
@Table(name = "meeting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting extends BaseTimeEntity {

	@Id
	@Column(length = 36)
	private String id;

	/**
	 * 회의 주최자(profile.id). 인증(auth) 연동 전까지 String으로 느슨하게 참조한다.
	 * TODO: Profile 엔티티 확정 후 FK 연관관계로 교체.
	 */
	@Column(name = "host_id", length = 36, nullable = false)
	private String hostId;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "expected_count")
	private Integer expectedCount;

	@Column(name = "invite_code", length = 12, nullable = false, unique = true)
	private String inviteCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MeetingStatus status;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "ended_at")
	private Instant endedAt;

	@Builder
	private Meeting(String hostId, String title, String description,
				   Integer expectedCount, String inviteCode) {
		this.hostId = hostId;
		this.title = title;
		this.description = description;
		this.expectedCount = expectedCount;
		this.inviteCode = inviteCode;
		this.status = MeetingStatus.WAITING;
	}

	@PrePersist
	private void assignId() {
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
	}

	/** 회의 종료 — 상태를 ENDED로, 종료 시각 기록. 이미 종료된 회의면 그대로 둔다(멱등). */
	public void end(Instant at) {
		if (this.status != MeetingStatus.ENDED) {
			this.status = MeetingStatus.ENDED;
			this.endedAt = at;
		}
	}
}
