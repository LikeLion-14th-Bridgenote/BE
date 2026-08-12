package com.bridgenote.common.jwt;

/**
 * 인증된 사용자. Supabase JWT의 sub(사용자 UUID)와 email 클레임에서 추출한다.
 * {@code id}가 곧 profile.id (= 회의의 host_id)이다.
 */
public record AuthUser(String id, String email) {
}
