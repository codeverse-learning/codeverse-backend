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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.security.MessageDigest;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final GoogleNonceService googleNonceService;
    private final RestClient restClient;

    private final String githubClientId;
    private final String githubClientSecret;
    private final String githubRedirectUri;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleIdTokenVerifier googleIdTokenVerifier,
            GoogleNonceService googleNonceService,
            @Value("${github.client-id}") String githubClientId,
            @Value("${github.client-secret}") String githubClientSecret,
            @Value("${github.redirect-uri:http://localhost:5173/github/callback}")
            String githubRedirectUri) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.googleNonceService = googleNonceService;

        // Configure HTTP connection timeout
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        // Configure HTTP read timeout
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        this.githubClientId = githubClientId;
        this.githubClientSecret = githubClientSecret;
        this.githubRedirectUri = githubRedirectUri;
    }

    // REGISTER
    public AuthResponse register(RegisterRequest request) {

        if (exceedsBcryptLimit(request.getPassword())) {
            throw new IllegalArgumentException(
                    "Password must not exceed 72 UTF-8 bytes"
            );
        }

        if (request.getPassword().length() < 15) {
            throw new IllegalArgumentException(
                    "Password must contain at least 15 characters"
            );
        }

        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Unable to create account. Please check your details."
            );
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role("STUDENT")
                .authProviders(new ArrayList<>(List.of("LOCAL")))
                .authProvider("LOCAL")
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = saveNewUser(user);

        return buildAuthResponse(
                savedUser,
                "Registration successful!"
        );
    }

    // EMAIL AND PASSWORD LOGIN
    public AuthResponse login(LoginRequest request) {

        if (exceedsBcryptLimit(request.getPassword())) {
            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

        String email = normalizeEmail(request.getEmail());

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
    public AuthResponse googleLogin(
            GoogleLoginRequest request,
            String sessionBinding
    ) {

        try {

            // Verify Google ID token signature and claims
            GoogleIdToken idToken =
                    googleIdTokenVerifier.verify(request.getIdToken());

            if (idToken == null) {
                throw new IllegalArgumentException(
                        "Invalid Google authentication"
                );
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            // Retrieve nonce from verified Google ID token
            String tokenNonce = safeString(payload.get("nonce"));

            String requestNonce = request.getNonce();

            // Validate nonce format
            if (
                    tokenNonce == null ||
                            requestNonce == null ||
                            !tokenNonce.matches("^[A-Za-z0-9_-]{43}$") ||
                            !requestNonce.matches("^[A-Za-z0-9_-]{43}$")
            ) {

                throw new IllegalArgumentException(
                        "Invalid Google authentication challenge"
                );
            }

            // Compare signed token nonce with request nonce
            boolean nonceMatches = MessageDigest.isEqual(
                    tokenNonce.getBytes(StandardCharsets.UTF_8),
                    requestNonce.getBytes(StandardCharsets.UTF_8)
            );

            if (!nonceMatches) {

                throw new IllegalArgumentException(
                        "Invalid Google authentication challenge"
                );
            }

            // Validate browser-session binding and consume nonce
            boolean validNonce =
                    googleNonceService.validateAndConsumeNonce(
                            requestNonce,
                            sessionBinding
                    );

            if (!validNonce) {

                throw new IllegalArgumentException(
                        "Google authentication challenge is invalid or expired"
                );
            }

            // Obtain immutable Google account ID
            String googleId = payload.getSubject();

            if (googleId == null ||
                    googleId.isBlank() ||
                    googleId.length() > 255) {

                throw new IllegalArgumentException(
                        "Invalid Google account identity"
                );
            }

            // Find existing user using Google ID
            User existingUser = userRepository
                    .findByGoogleId(googleId)
                    .orElse(null);

            if (existingUser != null) {

                if (!getAuthProviders(existingUser).contains("GOOGLE")) {

                    throw new IllegalArgumentException(
                            "Google authentication failed"
                    );
                }

                return buildAuthResponse(
                        existingUser,
                        "Google login successful!"
                );
            }

            // Verified email required for new accounts
            String email = payload.getEmail();

            if (email == null ||
                    !Boolean.TRUE.equals(payload.getEmailVerified())) {

                throw new IllegalArgumentException(
                        "Google email is not verified"
                );
            }

            email = normalizeEmail(email);

            // Prevent automatic account linking by email
            if (userRepository.existsByEmail(email)) {

                throw new IllegalArgumentException(
                        "Unable to authenticate. Please use your existing login method."
                );
            }

            String fullName = safeString(payload.get("name"));
            String profileImage = safeString(payload.get("picture"));

            // Create a new Google account
            User user = User.builder()
                    .fullName(resolveFullName(fullName, email))
                    .email(email)
                    .password(null)
                    .role("STUDENT")
                    .authProviders(new ArrayList<>(List.of("GOOGLE")))
                    .authProvider("GOOGLE")
                    .googleId(googleId)
                    .profileImage(profileImage)
                    .createdAt(LocalDateTime.now())
                    .build();

            User savedUser = saveNewUser(user);

            return buildAuthResponse(
                    savedUser,
                    "Google login successful!"
            );

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

            // PKCE verification
            tokenRequest.add("code_verifier", request.getCodeVerifier());

            Map<String, Object> tokenResponse = restClient.post()
                    .uri("https://github.com/login/oauth/access_token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(tokenRequest)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (tokenResponse == null ||
                    tokenResponse.containsKey("error")) {

                throw new IllegalArgumentException(
                        "GitHub authentication failed"
                );
            }

            String accessToken =
                    safeString(tokenResponse.get("access_token"));

            if (accessToken == null || accessToken.isBlank()) {

                throw new IllegalArgumentException(
                        "Unable to obtain GitHub access token"
                );
            }

            // Get authenticated GitHub profile
            Map<String, Object> githubUser = restClient.get()
                    .uri("https://api.github.com/user")
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + accessToken
                    )
                    .header(
                            HttpHeaders.ACCEPT,
                            "application/vnd.github+json"
                    )
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (githubUser == null) {

                throw new IllegalArgumentException(
                        "Unable to retrieve GitHub profile"
                );
            }

            // Obtain immutable GitHub account ID
            Object githubIdValue = githubUser.get("id");

            if (!(githubIdValue instanceof Number)) {

                throw new IllegalArgumentException(
                        "Invalid GitHub account identity"
                );
            }

            String githubId = githubIdValue.toString();

            if (!githubId.matches("[1-9][0-9]{0,19}")) {

                throw new IllegalArgumentException(
                        "Invalid GitHub account identity"
                );
            }

            // Find existing user using GitHub ID
            User existingUser = userRepository
                    .findByGithubId(githubId)
                    .orElse(null);

            if (existingUser != null) {

                if (!getAuthProviders(existingUser).contains("GITHUB")) {

                    throw new IllegalArgumentException(
                            "GitHub authentication failed"
                    );
                }

                return buildAuthResponse(
                        existingUser,
                        "GitHub login successful!"
                );
            }

            // Get verified primary email for new accounts
            List<Map<String, Object>> emails = restClient.get()
                    .uri("https://api.github.com/user/emails")
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + accessToken
                    )
                    .header(
                            HttpHeaders.ACCEPT,
                            "application/vnd.github+json"
                    )
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

                        email = safeString(emailData.get("email"));
                        break;
                    }
                }
            }

            if (email == null || email.isBlank()) {

                throw new IllegalArgumentException(
                        "No verified primary email found in your GitHub account"
                );
            }

            email = normalizeEmail(email);

            // Prevent automatic account linking by email
            if (userRepository.existsByEmail(email)) {

                throw new IllegalArgumentException(
                        "Unable to authenticate. Please use your existing login method."
                );
            }

            String githubName =
                    safeString(githubUser.get("name"));

            String githubUsername =
                    safeString(githubUser.get("login"));

            String profileImage =
                    safeString(githubUser.get("avatar_url"));

            String fullName =
                    githubName != null && !githubName.isBlank()
                            ? githubName
                            : githubUsername;

            // Create a new GitHub account
            User user = User.builder()
                    .fullName(resolveFullName(fullName, email))
                    .email(email)
                    .password(null)
                    .role("STUDENT")
                    .authProviders(new ArrayList<>(List.of("GITHUB")))
                    .authProvider("GITHUB")
                    .githubId(githubId)
                    .profileImage(profileImage)
                    .createdAt(LocalDateTime.now())
                    .build();

            User savedUser = saveNewUser(user);

            return buildAuthResponse(
                    savedUser,
                    "GitHub login successful!"
            );

        } catch (RestClientException e) {

            throw new IllegalArgumentException(
                    "Unable to authenticate with GitHub. Please try again."
            );
        }
    }

    // SAVE NEW USER AND HANDLE DUPLICATE KEYS
    private User saveNewUser(User user) {

        try {

            return userRepository.insert(user);

        } catch (DuplicateKeyException e) {

            throw new IllegalArgumentException(
                    "Unable to create account. Please use your existing login method."
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

        if (providers.isEmpty() && user.getPassword() != null) {

            providers.add("LOCAL");
        }

        return providers;
    }

    // NORMALIZE EMAIL
    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {

            throw new IllegalArgumentException(
                    "Invalid email address"
            );
        }

        String normalizedEmail = email
                .trim()
                .toLowerCase(Locale.ROOT);

        if (normalizedEmail.length() > 254 ||
                !normalizedEmail.matches(
                        "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
                )) {

            throw new IllegalArgumentException(
                    "Invalid email address"
            );
        }

        return normalizedEmail;
    }

    // SAFELY READ STRING VALUES FROM PROVIDER RESPONSES
    private String safeString(Object value) {

        return value instanceof String text ? text : null;
    }

    // RESOLVE DISPLAY NAME
    private String resolveFullName(String fullName, String email) {

        String resolvedName =
                fullName != null && !fullName.isBlank()
                        ? fullName.trim()
                        : email.split("@")[0];

        if (resolvedName.length() > 100) {

            resolvedName = resolvedName.substring(0, 100);
        }

        return resolvedName;
    }

    // BCrypt accepts at most 72 bytes, not 72 characters
    private boolean exceedsBcryptLimit(String password) {

        return password == null ||
                password.getBytes(StandardCharsets.UTF_8).length > 72;
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