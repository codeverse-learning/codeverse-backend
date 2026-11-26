package com.codeverse.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String fullName;

    @Indexed(unique = true)
    private String email;

    @ToString.Exclude
    private String password;

    @Builder.Default
    private String role = "STUDENT";

    @Builder.Default
    private List<String> authProviders =
            new ArrayList<>(List.of("LOCAL"));

    private String authProvider;

    // Immutable Google account identifier
    private String googleId;

    // Immutable GitHub account identifier
    private String githubId;

    private String profileImage;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}