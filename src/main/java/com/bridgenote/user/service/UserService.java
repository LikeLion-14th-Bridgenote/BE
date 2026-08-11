package com.bridgenote.user.service;

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
    public void deleteMyAccount(Long userId) {

        User user = findUser(userId);

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
}