package com.bridgenote.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

    // 아래 필드는 모두 users 테이블 NOT NULL 컬럼에 그대로 저장된다.
    // 하나라도 누락되면 Supabase Auth 계정만 생성되고 DB insert가 깨져 '고아 계정'이 남으므로,
    // Auth 호출 이전 단계(검증)에서 400으로 걸러낸다.
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    private String language;

    @NotBlank
    private String culture;

    @NotBlank
    private String job;

    // 선택 입력 (users.organization 은 nullable)
    private String organization;
}