package com.bridgenote.auth.service;

import com.bridgenote.auth.dto.*;
import com.bridgenote.common.exception.BusinessException;
import com.bridgenote.user.domain.User;
import com.bridgenote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bridgenote.auth.dto.SupabaseLoginResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final SupabaseAuthClient supabaseAuthClient;

    @Transactional
    public Long signup(SignupRequest request) {

        // 1. 우리 DB에서 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "이미 가입된 이메일입니다.");
        }

        // 2. Supabase Auth에 회원 생성
        SupabaseSignupResponse authResponse =
                supabaseAuthClient.signup(
                        request.getEmail(),
                        request.getPassword()
                );

        if (authResponse == null ||
                authResponse.getUser() == null) {

            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "회원가입에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        // Supabase Auth에서 생성된 UUID
        String authUserId =
                authResponse.getUser().getId();

        // 3. BridgeNote users 테이블에 프로필 저장
        //    이 단계가 실패하면 이미 만들어진 Auth 계정이 '고아'로 남으므로(트랜잭션 밖),
        //    실패 시 Auth 계정을 되돌려(deleteUser) 정합성을 맞춘다.
        try {
            User user = new User(
                    authUserId,
                    request.getEmail(),
                    request.getName(),
                    request.getLanguage(),
                    request.getCulture(),
                    request.getJob(),
                    request.getOrganization()
            );

            User savedUser =
                    userRepository.save(user);

            return savedUser.getId();

        } catch (RuntimeException e) {
            try {
                supabaseAuthClient.deleteUser(authUserId);
            } catch (RuntimeException cleanupError) {
                // 보상 삭제까지 실패하면 고아가 남을 수 있으니 로그로 남긴다(수동 정리 대상).
                log.error("프로필 저장 실패 후 Auth 계정 보상 삭제 실패. 고아 계정 수동 정리 필요: authUserId={}",
                        authUserId, cleanupError);
            }
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "회원 프로필 저장에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    public LoginResponse login(LoginRequest request) {

        // 1. Supabase Auth 로그인
        SupabaseLoginResponse authResponse =
                supabaseAuthClient.login(
                        request.getEmail(),
                        request.getPassword()
                );

        if (authResponse == null ||
                authResponse.getUser() == null ||
                authResponse.getAccessToken() == null) {

            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "로그인에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        // 2. BridgeNote users 테이블에서 프로필 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new BusinessException(HttpStatus.NOT_FOUND,
                                "프로필 정보가 존재하지 않습니다. 다시 회원가입해주세요.")
                );

        // 3. 실제 Supabase 토큰 반환
        return new LoginResponse(
                user.getId(),
                authResponse.getAccessToken(),
                authResponse.getRefreshToken()
        );
    }

    public void logout(String accessToken) {

        supabaseAuthClient.logout(accessToken);
    }
}