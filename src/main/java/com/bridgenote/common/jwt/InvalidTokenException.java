package com.bridgenote.common.jwt;

/**
 * JWT 검증 실패(서명 불일치·만료·형식 오류 등). → 401 응답.
 */
public class InvalidTokenException extends RuntimeException {

	public InvalidTokenException(String message) {
		super(message);
	}
}
