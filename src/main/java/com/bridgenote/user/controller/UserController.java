package com.bridgenote.user.controller;

import com.bridgenote.user.domain.User;
import com.bridgenote.user.dto.UserResponse;
import com.bridgenote.user.dto.UserUpdateRequest;
import com.bridgenote.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpHeaders;

import java.util.Map;

@Tag(name = "프로필", description = "회원 프로필 조회, 수정 및 탈퇴 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private String extractToken(String authorization) {

        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {

            throw new IllegalArgumentException(
                    "Authorization 헤더가 올바르지 않습니다."
            );
        }

        return authorization.substring(7);
    }

    @Operation(
            summary = "내 프로필 조회",
            description = "현재 로그인한 사용자의 프로필 정보를 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public UserResponse getMyProfile(
            @Parameter(hidden = true)
            @RequestHeader("Authorization") String authorization
    ) {

        String accessToken = extractToken(authorization);

        User user = userService.getCurrentUser(accessToken);

        return UserResponse.from(user);
    }

    @Operation(
            summary = "내 프로필 수정",
            description = "현재 로그인한 사용자의 이름, 언어, 문화권, 직무, 기관 정보를 수정합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @Parameter(hidden = true)
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody UserUpdateRequest request
    ) {

        String accessToken = extractToken(authorization);

        User user = userService.getCurrentUser(accessToken);

        return ResponseEntity.ok(
                userService.updateMyProfile(
                        user.getId(),
                        request
                )
        );
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자의 계정을 삭제하고 회원 탈퇴 처리합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyAccount(
            @Parameter(hidden = true)
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {

        String accessToken = extractToken(authorization);

        User user = userService.getCurrentUser(accessToken);

        userService.deleteMyAccount(user);

        return ResponseEntity.ok(
                Map.of(
                        "message", "회원 탈퇴 성공"
                )
        );
    }
}