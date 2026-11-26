package com.codeverse.service;

import com.codeverse.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.audience}")
    private String audience;

    private SecretKey signingKey;

    private JwtParser jwtParser;

    // Validate JWT configuration during application startup
    @PostConstruct
    public void initialize() {

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is missing."
            );
        }

        byte[] keyBytes;

        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "JWT secret must be valid Base64."
            );
        }

        // HS256 requires at least 256 bits
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must contain at least 32 bytes."
            );
        }

        // Reject an all-zero signing key
        boolean allZero = true;

        for (byte value : keyBytes) {
            if (value != 0) {
                allZero = false;
                break;
            }
        }

        if (allZero) {
            throw new IllegalStateException(
                    "JWT secret must not contain only zero bytes."
            );
        }

        // Validate token expiration configuration
        if (expiration < 60000L
                || expiration > 86400000L) {

            throw new IllegalStateException(
                    "Invalid JWT expiration configuration."
            );
        }

        if (issuer == null || issuer.isBlank()
                || audience == null || audience.isBlank()) {

            throw new IllegalStateException(
                    "JWT issuer and audience are required."
            );
        }

        signingKey = Keys.hmacShaKeyFor(keyBytes);

        // Build reusable JWT parser
        jwtParser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build();
    }

    // Generate JWT token
    public String generateToken(User user) {

        if (user == null
                || user.getId() == null
                || user.getId().isBlank()) {

            throw new IllegalArgumentException(
                    "Valid user identity is required."
            );
        }

        long currentTime = System.currentTimeMillis();

        return Jwts.builder()

                // Immutable user identity
                .subject(user.getId())

                // Token issuer
                .issuer(issuer)

                // Intended API audience
                .audience()
                .add(audience)
                .and()

                // Unique token identifier
                .id(UUID.randomUUID().toString())

                // Additional claims
                .claim("userId", user.getId())
                .claim("role", user.getRole())

                // Token validity period
                .issuedAt(new Date(currentTime))
                .expiration(
                        new Date(currentTime + expiration)
                )

                // Explicit signing algorithm
                .signWith(signingKey, Jwts.SIG.HS256)

                .compact();
    }

    // Validate JWT and extract claims once
    public Claims validateAndExtractClaims(String token) {

        if (token == null
                || token.isBlank()
                || token.length() > 4096) {

            throw new JwtException(
                    "Invalid authentication token."
            );
        }

        var signedToken = jwtParser.parseSignedClaims(token);

// Accept only HS256 signed tokens
        if (!"HS256".equals(
                signedToken.getHeader().getAlgorithm()
        )) {
            throw new JwtException(
                    "Unsupported token signing algorithm."
            );
        }

        Claims claims = signedToken.getPayload();

        String subject = claims.getSubject();

        String userId = claims.get(
                "userId",
                String.class
        );

        // Validate user identity
        if (subject == null
                || subject.isBlank()
                || !subject.equals(userId)) {

            throw new JwtException(
                    "Invalid token identity."
            );
        }

        // Validate required token identifier
        if (claims.getId() == null
                || claims.getId().isBlank()) {

            throw new JwtException(
                    "Invalid token identifier."
            );
        }

        Date issuedAt = claims.getIssuedAt();

        Date expiresAt = claims.getExpiration();

        Date now = new Date();

        // Validate required timestamps
        if (issuedAt == null
                || expiresAt == null
                || issuedAt.after(now)
                || !expiresAt.after(now)
                || !expiresAt.after(issuedAt)) {

            throw new JwtException(
                    "Invalid token validity period."
            );
        }

        return claims;
    }
}