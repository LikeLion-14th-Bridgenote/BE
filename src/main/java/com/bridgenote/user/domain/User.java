package com.bridgenote.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String culture;

    @Column(nullable = false)
    private String job;

    private String organization;

    public User(
            String email,
            String password,
            String name,
            String language,
            String culture,
            String job,
            String organization
    ) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.language = language;
        this.culture = culture;
        this.job = job;
        this.organization = organization;
    }

    public void updateProfile(
            String name,
            String language,
            String culture,
            String job,
            String organization
    ) {
        if (name != null) {
            this.name = name;
        }

        if (language != null) {
            this.language = language;
        }

        if (culture != null) {
            this.culture = culture;
        }

        if (job != null) {
            this.job = job;
        }

        if (organization != null) {

            if (organization.isBlank()) {
                this.organization = null;
            } else {
                this.organization = organization;
            }
        }
    }
}