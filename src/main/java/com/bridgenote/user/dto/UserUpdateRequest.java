package com.bridgenote.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {

    private String name;

    private String language;

    private String culture;

    private String job;

    private String organization;
}