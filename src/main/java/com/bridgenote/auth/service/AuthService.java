package com.bridgenote.auth.service;

import com.bridgenote.auth.dto.LoginRequest;
import com.bridgenote.auth.dto.LoginResponse;
import com.bridgenote.auth.dto.SignupRequest;
import com.bridgenote.user.domain.User;
import com.bridgenote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;

    @Transactional
    public Long signup(SignupRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        User user = new User(
                request.getEmail(),
                request.getPassword(),
                request.getName(),
                request.getLanguage(),
                request.getCulture(),
                request.getJob(),
                request.getOrganization()
        );

        User savedUser = userRepository.save(user);

        return savedUser.getId();
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 회원입니다.")
                );

        if (!user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // JWT 적용 전 임시 토큰
        String accessToken = UUID.randomUUID().toString();

        return new LoginResponse(
                user.getId(),
                accessToken
        );
    }

    public void logout() {
        // JWT/Supabase 적용 후 토큰 무효화 로직 추가
    }
}