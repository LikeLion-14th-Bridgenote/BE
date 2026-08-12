package com.bridgenote.auth.controller;

import com.bridgenote.auth.dto.LoginRequest;
import com.bridgenote.auth.dto.LoginResponse;
import com.bridgenote.auth.dto.SignupRequest;
import com.bridgenote.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "인증", description = "회원가입, 로그인, 로그아웃 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "회원가입",
            description = "이메일, 비밀번호, 이름, 언어, 문화권, 직무 등의 정보를 입력하여 회원가입합니다. 기관은 선택 입력입니다."
    )
    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @RequestBody SignupRequest request
    ) {

        Long userId = authService.signup(request);

        return ResponseEntity.ok(
                Map.of(
                        "message", "회원가입 성공",
                        "userId", userId
                )
        );
    }

    @Operation(
            summary = "로그인 및 토큰 발급",
            description = "이메일과 비밀번호로 로그인하고 Access Token을 발급받습니다."
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request
    ) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }

    @Operation(
            summary = "로그아웃",
            description = "현재 로그인된 사용자를 로그아웃 처리합니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {

        authService.logout();

        return ResponseEntity.ok(
                Map.of(
                        "message", "로그아웃 성공"
                )
        );
    }
}