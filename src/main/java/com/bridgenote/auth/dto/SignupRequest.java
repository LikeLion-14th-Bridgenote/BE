package com.bridgenote.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

    private String email;
    private String password;
    private String name;

    private String language;
    private String culture;
    private String job;

    // 선택 입력
    private String organization;
}