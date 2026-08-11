package com.bridgenote.user.controller;

import com.bridgenote.user.dto.UserResponse;
import com.bridgenote.user.dto.UserUpdateRequest;
import com.bridgenote.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "프로필", description = "회원 프로필 조회, 수정 및 탈퇴 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "내 프로필 조회",
            description = "현재 로그인한 사용자의 프로필 정보를 조회합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(
            @RequestHeader("X-USER-ID") Long userId
    ) {

        return ResponseEntity.ok(
                userService.getMyProfile(userId)
        );
    }

    @Operation(
            summary = "내 프로필 수정",
            description = "현재 로그인한 사용자의 이름, 언어, 문화권, 직무, 기관 정보를 수정합니다."
    )
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody UserUpdateRequest request
    ) {

        return ResponseEntity.ok(
                userService.updateMyProfile(
                        userId,
                        request
                )
        );
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자의 계정을 삭제하고 회원 탈퇴 처리합니다."
    )
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyAccount(
            @RequestHeader("X-USER-ID") Long userId
    ) {

        userService.deleteMyAccount(userId);

        return ResponseEntity.ok(
                Map.of(
                        "message", "회원 탈퇴 성공"
                )
        );
    }
}