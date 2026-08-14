package com.bridgenote.common.exception;

import com.bridgenote.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(
			MethodArgumentNotValidException e
	) {
		FieldError fieldError = e.getBindingResult().getFieldError();

		String message = fieldError != null
				? fieldError.getField() + ": " + fieldError.getDefaultMessage()
				: "유효성 검증에 실패했습니다.";

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error(message));
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(
			BusinessException e
	) {
		return ResponseEntity
				.status(e.getStatus())
				.body(ApiResponse.error(e.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(
			Exception e
	) {
		// 개발 중 실제 오류 확인
		e.printStackTrace();

		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error(
						"서버 오류가 발생했습니다: " + e.getMessage()
				));
	}
}