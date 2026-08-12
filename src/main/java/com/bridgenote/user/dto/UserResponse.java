package com.bridgenote.user.dto;

import com.bridgenote.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String name;

    private String language;
    private String culture;
    private String job;
    private String organization;

    public static UserResponse from(User user) {

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .language(user.getLanguage())
                .culture(user.getCulture())
                .job(user.getJob())
                .organization(user.getOrganization())
                .build();
    }
}