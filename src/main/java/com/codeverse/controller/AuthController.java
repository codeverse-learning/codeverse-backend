
package com.codeverse.controller;

import com.codeverse.dto.request.LoginRequest;
import com.codeverse.dto.request.RegisterRequest;
import com.codeverse.dto.request.GoogleLoginRequest;
import com.codeverse.dto.response.AuthResponse;
import com.codeverse.dto.response.UserProfileResponse;
import com.codeverse.entity.User;
import com.codeverse.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.codeverse.dto.request.GithubLoginRequest;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    // GOOGLE LOGIN
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request) {

        AuthResponse response = authService.googleLogin(request);

        return ResponseEntity.ok(response);
    }

    // GITHUB LOGIN
    @PostMapping("/github")
    public ResponseEntity<AuthResponse> githubLogin(
            @Valid @RequestBody GithubLoginRequest request) {

        AuthResponse response = authService.githubLogin(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal User user) {

        UserProfileResponse response = UserProfileResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .build();

        return ResponseEntity.ok(response);
    }
}
