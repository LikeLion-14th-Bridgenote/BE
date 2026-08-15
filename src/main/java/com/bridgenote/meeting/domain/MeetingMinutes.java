package com.bridgenote.meeting.domain;

import com.bridgenote.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회의별 생성 회의록(AI /ai/minutes 결과). meeting_id = PK(회의당 1행).
 * minutes 배치가 비동기라 상태(PENDING→READY/FAILED)를 함께 보관한다.
 * 실제 섹션 데이터는 AI 응답 그대로 JSON 문자열({@link #minutesJson})로 저장한다(pass-through).
 */
@Entity
@Table(name = "meeting_minutes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingMinutes extends BaseTimeEntity {

	public enum Status {PENDING, READY, FAILED}

	@Id
	@Column(name = "meeting_id", length = 36)
	private String meetingId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status;

	/** AI 응답의 minutes 배열을 직렬화한 JSON. READY일 때만 채워진다. */
	@Column(name = "minutes_json", columnDefinition = "text")
	private String minutesJson;

	public MeetingMinutes(String meetingId) {
		this.meetingId = meetingId;
		this.status = Status.PENDING;
	}

	/** 재생성/최초 트리거 — 상태를 대기로 되돌리고 이전 결과를 비운다. */
	public void markPending() {
		this.status = Status.PENDING;
		this.minutesJson = null;
	}

	public void markReady(String minutesJson) {
		this.status = Status.READY;
		this.minutesJson = minutesJson;
	}

	public void markFailed() {
		this.status = Status.FAILED;
	}
}
