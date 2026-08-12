package com.bridgenote.common.response;

/**
 * 표준 에러 응답. API 스펙 규약: {@code { "message": "..." }}
 */
public record ErrorResponse(String message) {
}
