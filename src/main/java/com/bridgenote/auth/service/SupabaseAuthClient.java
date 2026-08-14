package com.bridgenote.auth.service;

import com.bridgenote.auth.config.SupabaseProperties;
import com.bridgenote.auth.dto.SupabaseLoginResponse;
import com.bridgenote.auth.dto.SupabaseSignupResponse;
import com.bridgenote.auth.dto.SupabaseUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SupabaseAuthClient {

    private final SupabaseProperties supabaseProperties;

    private WebClient webClient() {
        return WebClient.builder()
                .baseUrl(supabaseProperties.getUrl())
                .defaultHeader(
                        "apikey",
                        supabaseProperties.getPublishableKey()
                )
                .build();
    }

    public SupabaseSignupResponse signup(
            String email,
            String password
    ) {
        return webClient()
                .post()
                .uri("/auth/v1/signup")
                .bodyValue(
                        Map.of(
                                "email", email,
                                "password", password
                        )
                )
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> new RuntimeException(
                                        "Supabase signup error: " + body
                                ))
                )
                .bodyToMono(SupabaseSignupResponse.class)
                .block();
    }

    public SupabaseLoginResponse login(
            String email,
            String password
    ) {
        return webClient()
                .post()
                .uri("/auth/v1/token?grant_type=password")
                .bodyValue(
                        Map.of(
                                "email", email,
                                "password", password
                        )
                )
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> new RuntimeException(
                                        "Supabase login error: " + body
                                ))
                )
                .bodyToMono(SupabaseLoginResponse.class)
                .block();
    }

    public SupabaseUserResponse getUser(String accessToken) {

        return webClient()
                .get()
                .uri("/auth/v1/user")
                .header(
                        "Authorization",
                        "Bearer " + accessToken
                )
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> new RuntimeException(
                                        "Supabase user error: " + body
                                ))
                )
                .bodyToMono(SupabaseUserResponse.class)
                .block();
    }

    public void logout(String accessToken) {

        webClient()
                .post()
                .uri("/auth/v1/logout")
                .header(
                        "Authorization",
                        "Bearer " + accessToken
                )
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> new RuntimeException(
                                        "Supabase logout error: " + body
                                ))
                )
                .toBodilessEntity()
                .block();
    }

    public void deleteUser(String authUserId) {

        WebClient adminClient = WebClient.builder()
                .baseUrl(supabaseProperties.getUrl())
                .defaultHeader(
                        "apikey",
                        supabaseProperties.getSecretKey()
                )
                .defaultHeader(
                        "Authorization",
                        "Bearer " + supabaseProperties.getSecretKey()
                )
                .build();

        adminClient
                .delete()
                .uri("/auth/v1/admin/users/{id}", authUserId)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> new RuntimeException(
                                        "Supabase delete user error: " + body
                                ))
                )
                .toBodilessEntity()
                .block();
    }
}