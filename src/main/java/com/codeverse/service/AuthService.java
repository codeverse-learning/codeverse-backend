
package com.codeverse.service;

import com.codeverse.dto.request.LoginRequest;
import com.codeverse.dto.request.RegisterRequest;
import com.codeverse.dto.request.GoogleLoginRequest;
import com.codeverse.dto.request.GithubLoginRequest;
import com.codeverse.dto.response.AuthResponse;
import com.codeverse.entity.User;
import com.codeverse.repository.UserRepository;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final RestClient restClient;

    private final String githubClientId;
    private final String githubClientSecret;
    private final String githubRedirectUri;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleIdTokenVerifier googleIdTokenVerifier,
            @Value("${github.client-id}") String githubClientId,
            @Value("${github.client-secret}") String githubClientSecret,
            @Value("${github.redirect-uri:http://localhost:5173/github/callback}")
            String githubRedirectUri) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.restClient = RestClient.builder().build();

        this.githubClientId = githubClientId;
        this.githubClientSecret = githubClientSecret;
        this.githubRedirectUri = githubRedirectUri;
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
                .authProviders(new ArrayList<>(List.of("LOCAL")))
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        return buildAuthResponse(savedUser, "Registration successful!");
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

        return buildAuthResponse(user, "Login successful!");
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
                        .authProviders(new ArrayList<>(List.of("GOOGLE")))
                        .profileImage(profileImage)
                        .createdAt(LocalDateTime.now())
                        .build();

                user = userRepository.save(user);

            } else {

                List<String> providers = getAuthProviders(user);

                if (providers.contains("LOCAL") &&
                        !providers.contains("GOOGLE")) {

                    throw new IllegalArgumentException(
                            "This email already has a password account. Please use your existing login method."
                    );
                }

                if (!providers.contains("GOOGLE")) {
                    providers.add("GOOGLE");
                    user.setAuthProviders(providers);
                    user = userRepository.save(user);
                }
            }

            return buildAuthResponse(user, "Google login successful!");

        } catch (GeneralSecurityException | IOException e) {

            throw new IllegalArgumentException(
                    "Unable to verify Google account"
            );
        }
    }

    // GITHUB LOGIN
    public AuthResponse githubLogin(GithubLoginRequest request) {

        try {

            // Exchange authorization code for access token
            MultiValueMap<String, String> tokenRequest =
                    new LinkedMultiValueMap<>();

            tokenRequest.add("client_id", githubClientId);
            tokenRequest.add("client_secret", githubClientSecret);
            tokenRequest.add("code", request.getCode());
            tokenRequest.add("redirect_uri", githubRedirectUri);

            Map<String, Object> tokenResponse = restClient.post()
                    .uri("https://github.com/login/oauth/access_token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(tokenRequest)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (tokenResponse == null ||
                    tokenResponse.get("access_token") == null) {

                throw new IllegalArgumentException(
                        "Unable to obtain GitHub access token"
                );
            }

            String accessToken =
                    tokenResponse.get("access_token").toString();

            // Get GitHub profile
            Map<String, Object> githubUser = restClient.get()
                    .uri("https://api.github.com/user")
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + accessToken
                    )
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (githubUser == null) {
                throw new IllegalArgumentException(
                        "Unable to retrieve GitHub profile"
                );
            }

            // Get verified primary email
            List<Map<String, Object>> emails = restClient.get()
                    .uri("https://api.github.com/user/emails")
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + accessToken
                    )
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            String email = null;

            if (emails != null) {
                for (Map<String, Object> emailData : emails) {

                    boolean primary =
                            Boolean.TRUE.equals(emailData.get("primary"));

                    boolean verified =
                            Boolean.TRUE.equals(emailData.get("verified"));

                    if (primary && verified) {
                        email = (String) emailData.get("email");
                        break;
                    }
                }
            }

            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException(
                        "No verified primary email found in your GitHub account."
                );
            }

            email = email.trim().toLowerCase(Locale.ROOT);

            String githubName = (String) githubUser.get("name");
            String githubUsername = (String) githubUser.get("login");
            String profileImage = (String) githubUser.get("avatar_url");

            String fullName =
                    githubName != null && !githubName.isBlank()
                            ? githubName
                            : githubUsername;

            if (fullName == null || fullName.isBlank()) {
                fullName = email.split("@")[0];
            }

            // Find existing CODEVERSE account
            User user = userRepository.findByEmail(email)
                    .orElse(null);

            if (user == null) {

                // Create new GitHub account
                user = User.builder()
                        .fullName(fullName)
                        .email(email)
                        .password(null)
                        .role("STUDENT")
                        .authProviders(new ArrayList<>(List.of("GITHUB")))
                        .profileImage(profileImage)
                        .createdAt(LocalDateTime.now())
                        .build();

                user = userRepository.save(user);

            } else {

                List<String> providers = getAuthProviders(user);

                // Only allow automatic linking to an existing Google account
                if (!providers.contains("GOOGLE") &&
                        !providers.contains("GITHUB")) {

                    throw new IllegalArgumentException(
                            "An account with this email already exists. Please login using your existing method."
                    );
                }

                if (!providers.contains("GITHUB")) {
                    providers.add("GITHUB");
                    user.setAuthProviders(providers);
                    user = userRepository.save(user);
                }
            }

            return buildAuthResponse(
                    user,
                    "GitHub login successful!"
            );

        } catch (RestClientException e) {

            throw new IllegalArgumentException(
                    "Unable to authenticate with GitHub. Please try again."
            );
        }
    }

    // GET PROVIDERS AND SUPPORT OLD MONGODB DOCUMENTS
    private List<String> getAuthProviders(User user) {

        List<String> providers = user.getAuthProviders() == null
                ? new ArrayList<>()
                : new ArrayList<>(user.getAuthProviders());

        String legacyProvider = user.getAuthProvider();

        if (legacyProvider != null && !legacyProvider.isBlank()) {

            // Migrate old Google/GitHub accounts
            if (providers.isEmpty()
                    || (providers.size() == 1
                    && "LOCAL".equals(providers.get(0))
                    && !"LOCAL".equals(legacyProvider))) {

                providers.clear();
                providers.add(legacyProvider);

            } else if (!providers.contains(legacyProvider)) {
                providers.add(legacyProvider);
            }
        }

        if (providers.isEmpty()) {
            providers.add("LOCAL");
        }

        user.setAuthProviders(providers);

        return providers;
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
