package com.bridgenote.meeting.domain;

/**
 * 회의 상태.
 * <ul>
 *   <li>{@code WAITING} — 생성됨, 시작 대기</li>
 *   <li>{@code LIVE} — 진행 중</li>
 *   <li>{@code ENDED} — 종료</li>
 * </ul>
 */
public enum MeetingStatus {
	WAITING, LIVE, ENDED
}
