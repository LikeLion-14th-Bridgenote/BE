package com.bridgenote.meeting.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 데이터 처리 동의 요청.
 */
public record MeetingConsentReqDto(
		@NotNull
		Boolean agreed
) {
}
