package com.bridgenote.common.exception;

import com.bridgenote.common.jwt.InvalidTokenException;
import com.bridgenote.common.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 핸들러. 모든 에러를 스펙 규약 {@code { "message": "..." }}으로 변환한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/** @Valid 검증 실패 (400) */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
		return ResponseEntity.badRequest().body(new ErrorResponse(
				"잘못된 요청입니다. 필수 값이 누락되었거나 잘못된 형식의 데이터가 포함되어 있습니다."));
	}

	/** 본문 파싱 실패(형식 오류·인코딩 등) (400) */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
		return ResponseEntity.badRequest().body(new ErrorResponse(
				"잘못된 요청입니다. 필수 값이 누락되었거나 잘못된 형식의 데이터가 포함되어 있습니다."));
	}

	/** 인증 실패/미인증 (401) */
	@ExceptionHandler(InvalidTokenException.class)
	public ResponseEntity<ErrorResponse> handleUnauthorized(InvalidTokenException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage()));
	}

	/** 도메인 비즈니스 예외 (상태코드는 예외가 지정) */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
		log.warn("BusinessException: {} ({})", e.getMessage(), e.getStatus());
		return ResponseEntity.status(e.getStatus()).body(new ErrorResponse(e.getMessage()));
	}

	/** 그 외 예기치 못한 예외 (500) — 반드시 스택트레이스를 남긴다. */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e) {
		log.error("처리되지 않은 예외", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("서버에서 오류가 발생했습니다."));
	}
}
