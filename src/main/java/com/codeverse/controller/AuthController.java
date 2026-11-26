package com.codeverse.controller;

import com.codeverse.dto.request.LoginRequest;
import com.codeverse.dto.request.RegisterRequest;
import com.codeverse.dto.request.GoogleLoginRequest;
import com.codeverse.dto.request.GithubLoginRequest;

import com.codeverse.dto.response.AuthResponse;
import com.codeverse.dto.response.UserProfileResponse;

import com.codeverse.entity.User;

import com.codeverse.service.AuthService;
import com.codeverse.service.GoogleNonceService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")

public class AuthController {

    private static final String GOOGLE_SESSION_COOKIE =
            "codeverse_google_session";

    private final AuthService authService;

    private final GoogleNonceService googleNonceService;

    @Value("${codeverse.auth.cookie-secure:true}")
    private boolean secureCookie;

    @Value("${codeverse.auth.allowed-origin:http://localhost:5173}")
    private String allowedOrigin;

    public AuthController(
            AuthService authService,
            GoogleNonceService googleNonceService
    ) {
        this.authService = authService;
        this.googleNonceService = googleNonceService;
    }

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        AuthResponse response =
                authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        AuthResponse response =
                authService.login(request);

        return ResponseEntity.ok(response);
    }

    // GENERATE GOOGLE AUTHENTICATION CHALLENGE
    @PostMapping("/google/nonce")
    public ResponseEntity<Map<String, String>> generateGoogleNonce(
            @CookieValue(
                    name = GOOGLE_SESSION_COOKIE,
                    required = false
            ) String existingSession,
            HttpServletRequest httpRequest
    ) {

        validateGoogleOrigin(httpRequest);

        String sessionBinding = existingSession;

        // Generate a secure browser-session binding
        if (
                sessionBinding == null ||
                        !sessionBinding.matches("^[A-Za-z0-9_-]{43}$")
        ) {

            sessionBinding =
                    googleNonceService.generateSessionBinding();
        }

        String nonce;

        try {

            // Generate a single-use authentication nonce
            nonce = googleNonceService.generateNonce(
                    sessionBinding
            );

        } catch (IllegalStateException exception) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Authentication temporarily unavailable."
            );
        }

        // Store browser-session binding in HttpOnly cookie
        ResponseCookie sessionCookie = ResponseCookie
                .from(GOOGLE_SESSION_COOKIE, sessionBinding)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/api/auth/google")
                .maxAge(Duration.ofMinutes(10))
                .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        sessionCookie.toString()
                )
                .cacheControl(CacheControl.noStore())
                .body(Map.of("nonce", nonce));
    }

    // GOOGLE LOGIN
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,

            @CookieValue(
                    name = GOOGLE_SESSION_COOKIE,
                    required = false
            ) String sessionBinding,

            HttpServletRequest httpRequest
    ) {

        validateGoogleOrigin(httpRequest);

        // Reject missing or invalid browser session
        if (
                sessionBinding == null ||
                        !sessionBinding.matches("^[A-Za-z0-9_-]{43}$")
        ) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid Google authentication session."
            );
        }

        // Verify Google token and consume nonce
        AuthResponse response = authService.googleLogin(
                request,
                sessionBinding
        );

        // Clear temporary session cookie after successful login
        ResponseCookie clearCookie = ResponseCookie
                .from(GOOGLE_SESSION_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/api/auth/google")
                .maxAge(Duration.ZERO)
                .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearCookie.toString()
                )
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    // GITHUB LOGIN
    @PostMapping("/github")
    public ResponseEntity<AuthResponse> githubLogin(
            @Valid @RequestBody GithubLoginRequest request
    ) {

        AuthResponse response =
                authService.githubLogin(request);

        return ResponseEntity.ok(response);
    }

    // GET CURRENT USER
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal User user
    ) {

        UserProfileResponse response =
                UserProfileResponse.builder()
                        .userId(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .authProvider(user.getAuthProvider())
                        .build();

        return ResponseEntity.ok(response);
    }

    // VALIDATE GOOGLE AUTHENTICATION REQUEST ORIGIN
    private void validateGoogleOrigin(
            HttpServletRequest request
    ) {

        String origin = request.getHeader(HttpHeaders.ORIGIN);

        if (origin == null || !allowedOrigin.equals(origin)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Untrusted authentication request origin."
            );
        }
    }
}