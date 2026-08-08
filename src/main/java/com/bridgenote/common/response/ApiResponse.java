package com.bridgenote.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 표준 API 응답 래퍼.
 * <pre>
 * { "success": true,  "message": null, "data": { ... } }
 * { "success": false, "message": "에러 메시지", "data": null }
 * </pre>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

	private final boolean success;
	private final String message;
	private final T data;

	private ApiResponse(boolean success, String message, T data) {
		this.success = success;
		this.message = message;
		this.data = data;
	}

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, null, data);
	}

	public static <T> ApiResponse<T> success(String message, T data) {
		return new ApiResponse<>(true, message, data);
	}

	public static ApiResponse<Void> error(String message) {
		return new ApiResponse<>(false, message, null);
	}
}
