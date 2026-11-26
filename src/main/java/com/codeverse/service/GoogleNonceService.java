package com.codeverse.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class GoogleNonceService {

    // Nonce validity period
    private static final Duration NONCE_VALIDITY =
            Duration.ofMinutes(5);

    // Maximum number of pending challenges
    private static final int MAX_PENDING_NONCES = 10000;

    // Cryptographically secure random generator
    private final SecureRandom secureRandom =
            new SecureRandom();

    // Clock for production and automated testing
    private final Clock clock;

    // Temporary nonce storage
    private final Map<String, NonceChallenge> pendingNonces =
            new HashMap<>();

    // Default constructor for Spring Boot
    public GoogleNonceService() {
        this(Clock.systemUTC());
    }

    // Constructor for automated testing
    GoogleNonceService(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    // Internal nonce challenge
    private record NonceChallenge(
            String sessionBinding,
            Instant expiresAt
    ) {
    }

    // Generate secure random Base64URL value
    private String generateSecureValue() {

        byte[] randomBytes = new byte[32];

        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    // Generate a new browser-session binding
    public String generateSessionBinding() {

        return generateSecureValue();
    }

    // Validate secure value format
    private boolean isValidSecureValue(String value) {

        return value != null
                && value.length() == 43
                && value.matches("^[A-Za-z0-9_-]+$");
    }

    // Generate and store a new nonce
    public synchronized String generateNonce(
            String sessionBinding
    ) {

        if (!isValidSecureValue(sessionBinding)) {
            throw new IllegalArgumentException(
                    "Invalid authentication session."
            );
        }

        // Remove expired challenges
        removeExpiredNonces();

        // Prevent unlimited memory usage
        if (pendingNonces.size() >= MAX_PENDING_NONCES) {
            throw new IllegalStateException(
                    "Authentication challenge capacity reached."
            );
        }

        String nonce;

        // Ensure nonce uniqueness
        do {
            nonce = generateSecureValue();
        } while (pendingNonces.containsKey(nonce));

        Instant expiresAt =
                clock.instant().plus(NONCE_VALIDITY);

        pendingNonces.put(
                nonce,
                new NonceChallenge(
                        sessionBinding,
                        expiresAt
                )
        );

        return nonce;
    }

    // Validate and consume a nonce exactly once
    public synchronized boolean validateAndConsumeNonce(
            String nonce,
            String sessionBinding
    ) {

        if (!isValidSecureValue(nonce)
                || !isValidSecureValue(sessionBinding)) {
            return false;
        }

        NonceChallenge challenge =
                pendingNonces.get(nonce);

        // Reject unknown or previously consumed nonce
        if (challenge == null) {
            return false;
        }

        // Reject expired nonce
        if (!clock.instant().isBefore(challenge.expiresAt())) {

            pendingNonces.remove(nonce);

            return false;
        }

        // Verify browser-session binding
        boolean validSession = MessageDigest.isEqual(
                challenge.sessionBinding()
                        .getBytes(StandardCharsets.UTF_8),

                sessionBinding
                        .getBytes(StandardCharsets.UTF_8)
        );

        if (!validSession) {
            return false;
        }

        // Consume nonce to prevent reuse
        pendingNonces.remove(nonce);

        return true;
    }

    // Remove expired nonce challenges
    private void removeExpiredNonces() {

        Instant now = clock.instant();

        pendingNonces.entrySet().removeIf(
                entry -> !now.isBefore(
                        entry.getValue().expiresAt()
                )
        );
    }
}