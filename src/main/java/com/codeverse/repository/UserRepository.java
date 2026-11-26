package com.codeverse.repository;

import com.codeverse.entity.User;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    // Find user by email
    Optional<User> findByEmail(String email);

    // Check whether email already exists
    boolean existsByEmail(String email);

    // Find user by immutable Google account ID
    Optional<User> findByGoogleId(String googleId);

    // Find user by immutable GitHub account ID
    Optional<User> findByGithubId(String githubId);
}