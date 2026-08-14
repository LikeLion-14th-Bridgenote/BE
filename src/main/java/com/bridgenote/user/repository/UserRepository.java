package com.bridgenote.user.repository;

import com.bridgenote.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByAuthUserId(String authUserId);

    boolean existsByEmail(String email);
}