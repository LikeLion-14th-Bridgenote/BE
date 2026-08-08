package com.bridgenote.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 도메인 비즈니스 예외. 상태코드와 사용자 메시지를 함께 담는다.
 */
@Getter
public class BusinessException extends RuntimeException {

	private final HttpStatus status;

	public BusinessException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}
}
