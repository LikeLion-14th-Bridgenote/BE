package com.bridgenote.user.service;

import com.bridgenote.auth.dto.SupabaseUserResponse;
import com.bridgenote.auth.service.SupabaseAuthClient;
import com.bridgenote.user.domain.User;
import com.bridgenote.user.dto.UserResponse;
import com.bridgenote.user.dto.UserUpdateRequest;
import com.bridgenote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final SupabaseAuthClient supabaseAuthClient;

    public UserResponse getMyProfile(Long userId) {

        User user = findUser(userId);

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMyProfile(
            Long userId,
            UserUpdateRequest request
    ) {

        User user = findUser(userId);

        user.updateProfile(
                request.getName(),
                request.getLanguage(),
                request.getCulture(),
                request.getJob(),
                request.getOrganization()
        );

        return UserResponse.from(user);
    }

    @Transactional
    public void deleteMyAccount(User user) {

        supabaseAuthClient.deleteUser(
                user.getAuthUserId()
        );

        userRepository.delete(user);
    }

    private User findUser(Long userId) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 회원입니다."
                        )
                );
    }

    public User getCurrentUser(String accessToken) {

        SupabaseUserResponse authUser =
                supabaseAuthClient.getUser(accessToken);

        if (authUser == null || authUser.getId() == null) {
            throw new IllegalArgumentException(
                    "유효하지 않은 사용자입니다."
            );
        }

        return userRepository
                .findByAuthUserId(authUser.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자 프로필을 찾을 수 없습니다."
                        )
                );
    }
}