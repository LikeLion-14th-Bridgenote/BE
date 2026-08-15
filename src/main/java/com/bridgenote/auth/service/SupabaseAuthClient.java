package com.bridgenote.auth.service;

import com.bridgenote.auth.config.SupabaseProperties;
import com.bridgenote.auth.dto.SupabaseLoginResponse;
import com.bridgenote.auth.dto.SupabaseSignupResponse;
import com.bridgenote.auth.dto.SupabaseUserResponse;
import com.bridgenote.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
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
                        response -> response.bodyToMono(String.class).defaultIfEmpty("")
                                .map(body -> authError(response.statusCode(), body))
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
                        response -> response.bodyToMono(String.class).defaultIfEmpty("")
                                .map(body -> authError(response.statusCode(), body))
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
                        response -> response.bodyToMono(String.class).defaultIfEmpty("")
                                .map(body -> authError(response.statusCode(), body))
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
                        response -> response.bodyToMono(String.class).defaultIfEmpty("")
                                .map(body -> authError(response.statusCode(), body))
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
                        response -> response.bodyToMono(String.class).defaultIfEmpty("")
                                .map(body -> authError(response.statusCode(), body))
                )
                .toBodilessEntity()
                .block();
    }

    /**
     * Supabase 에러 응답(status + body)을 적절한 HTTP 상태의 {@link BusinessException}으로 변환.
     * 이렇게 던지면 GlobalExceptionHandler가 그 상태로 응답한다(예전엔 전부 500이었음).
     */
    private BusinessException authError(HttpStatusCode status, String body) {
        String code = field(body, "error_code");
        String msg = field(body, "msg");

        if ("invalid_credentials".equals(code)) {
            return new BusinessException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        if ("email_not_confirmed".equals(code)) {
            return new BusinessException(HttpStatus.UNAUTHORIZED, "이메일 인증이 필요합니다. 메일함을 확인해주세요.");
        }
        if ("user_already_exists".equals(code) || "email_exists".equals(code)
                || (msg != null && msg.toLowerCase().contains("already registered"))) {
            return new BusinessException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }
        if ("weak_password".equals(code)) {
            return new BusinessException(HttpStatus.BAD_REQUEST, "비밀번호가 정책에 맞지 않습니다. 더 강력한 비밀번호를 사용해주세요.");
        }
        if (status.value() == 400 || status.value() == 422) {
            return new BusinessException(HttpStatus.BAD_REQUEST, (msg != null && !msg.isBlank()) ? msg : "잘못된 인증 요청입니다.");
        }
        log.warn("Supabase 인증 upstream 오류 status={} body={}", status, body);
        return new BusinessException(HttpStatus.BAD_GATEWAY, "인증 서버와 통신 중 오류가 발생했습니다.");
    }

    /** JSON body에서 "key":"value" 문자열 필드를 단순 추출(에러 응답 파싱용). */
    private static String field(String json, String key) {
        if (json == null) {
            return null;
        }
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }
}