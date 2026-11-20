
package com.codeverse.service;

import com.codeverse.dto.request.LoginRequest;
import com.codeverse.dto.request.RegisterRequest;
import com.codeverse.dto.request.GoogleLoginRequest;
import com.codeverse.dto.response.AuthResponse;
import com.codeverse.entity.User;
import com.codeverse.repository.UserRepository;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleIdTokenVerifier googleIdTokenVerifier) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
    }

    // REGISTER
    public AuthResponse register(RegisterRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role("STUDENT")
                .authProvider("LOCAL")
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        return buildAuthResponse(
                savedUser,
                "Registration successful!"
        );
    }

    // EMAIL AND PASSWORD LOGIN
    public AuthResponse login(LoginRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid email or password"
                ));

        if (user.getPassword() == null ||
                !passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                )) {

            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

        return buildAuthResponse(
                user,
                "Login successful!"
        );
    }

    // GOOGLE LOGIN
    public AuthResponse googleLogin(GoogleLoginRequest request) {

        try {

            GoogleIdToken idToken =
                    googleIdTokenVerifier.verify(request.getIdToken());

            if (idToken == null) {
                throw new IllegalArgumentException(
                        "Invalid Google ID token"
                );
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();

            if (email == null ||
                    !Boolean.TRUE.equals(payload.getEmailVerified())) {

                throw new IllegalArgumentException(
                        "Google email is not verified"
                );
            }

            email = email.trim().toLowerCase(Locale.ROOT);

            String fullName = (String) payload.get("name");
            String profileImage = (String) payload.get("picture");

            User user = userRepository.findByEmail(email)
                    .orElse(null);

            if (user == null) {

                user = User.builder()
                        .fullName(
                                fullName != null && !fullName.isBlank()
                                        ? fullName
                                        : email.split("@")[0]
                        )
                        .email(email)
                        .password(null)
                        .role("STUDENT")
                        .authProvider("GOOGLE")
                        .profileImage(profileImage)
                        .createdAt(LocalDateTime.now())
                        .build();

                user = userRepository.save(user);

            } else if (!"GOOGLE".equals(user.getAuthProvider())) {

                throw new IllegalArgumentException(
                        "An account with this email already exists. Please login using your password."
                );
            }

            return buildAuthResponse(
                    user,
                    "Google login successful!"
            );

        } catch (GeneralSecurityException | IOException e) {

            throw new IllegalArgumentException(
                    "Unable to verify Google account"
            );
        }
    }

    // COMMON AUTH RESPONSE
    private AuthResponse buildAuthResponse(
            User user,
            String message) {

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .message(message)
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
