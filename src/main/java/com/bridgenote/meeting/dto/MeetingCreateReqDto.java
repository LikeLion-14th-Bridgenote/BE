package com.bridgenote.meeting.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 회의 생성 요청. (JSON은 snake_case: expected_count)
 */
public record MeetingCreateReqDto(

		@NotBlank
		String title,

		String description,

		@Min(1)
		Integer expectedCount
) {
}
