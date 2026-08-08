package com.bridgenote.common.exception;

import com.bridgenote.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 핸들러. 모든 예외를 표준 응답({@link ApiResponse})으로 변환한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/** @Valid 검증 실패 (400) */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
		FieldError fieldError = e.getBindingResult().getFieldError();
		String message = fieldError != null
				? fieldError.getField() + ": " + fieldError.getDefaultMessage()
				: "유효성 검증에 실패했습니다.";
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
	}

	/** 도메인 비즈니스 예외 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
		return ResponseEntity.status(e.getStatus()).body(ApiResponse.error(e.getMessage()));
	}

	/** 그 외 예기치 못한 예외 (500) */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("서버 오류가 발생했습니다."));
	}
}
