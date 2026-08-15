package com.bridgenote.auth.service;

import com.bridgenote.auth.dto.*;
import com.bridgenote.user.domain.User;
import com.bridgenote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bridgenote.auth.dto.SupabaseLoginResponse;

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
            throw new IllegalArgumentException(
                    "이미 존재하는 이메일입니다."
            );
        }

        // 2. Supabase Auth에 회원 생성
        SupabaseSignupResponse authResponse =
                supabaseAuthClient.signup(
                        request.getEmail(),
                        request.getPassword()
                );

        if (authResponse == null ||
                authResponse.getUser() == null) {

            throw new IllegalStateException(
                    "Supabase 회원가입에 실패했습니다."
            );
        }

        // Supabase Auth에서 생성된 UUID
        String authUserId =
                authResponse.getUser().getId();

        // 3. BridgeNote users 테이블에 프로필 저장
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

            throw new IllegalStateException(
                    "Supabase 로그인에 실패했습니다."
            );
        }

        // 2. BridgeNote users 테이블에서 프로필 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "프로필 정보가 존재하지 않습니다."
                        )
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