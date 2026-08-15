package com.bridgenote.auth.dto;

import lombok.Getter;

@Getter
public class SupabaseSignupResponse {

    private SupabaseUser user;

    @Getter
    public static class SupabaseUser {
        private String id;
        private String email;
    }
}