package com.codeverse.config;

import com.codeverse.entity.User;
import com.codeverse.repository.UserRepository;
import com.codeverse.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        String path = request.getServletPath();

        // Skip browser preflight requests
        if ("OPTIONS".equalsIgnoreCase(
                request.getMethod()
        )) {
            return true;
        }

        // Skip JWT validation for public authentication requests
        return "POST".equalsIgnoreCase(request.getMethod())
                && (
                path.equals("/api/auth/login")
                        || path.equals("/api/auth/register")
                        || path.equals("/api/auth/google")
                        || path.equals("/api/auth/google/nonce")
                        || path.equals("/api/auth/github")
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader =
                request.getHeader("Authorization");

        // Continue without authentication when token is missing
        if (authHeader == null
                || !authHeader.regionMatches(
                true,
                0,
                "Bearer ",
                0,
                7
        )) {

            filterChain.doFilter(request, response);
            return;
        }

        // Avoid unnecessary authentication processing
        if (SecurityContextHolder.getContext()
                .getAuthentication() != null) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        Claims claims;

        try {

            // Validate and parse JWT only once
            claims = jwtService
                    .validateAndExtractClaims(token);

        } catch (JwtException | IllegalArgumentException e) {

            // Reject invalid or expired tokens
            SecurityContextHolder.clearContext();

            filterChain.doFilter(request, response);
            return;
        }

        // Extract immutable user ID
        String userId = claims.getSubject();

        // Load current user from database
        User user = userRepository
                .findById(userId)
                .orElse(null);

        // Authenticate only existing users with supported roles
        if (user != null
                && (
                "STUDENT".equals(user.getRole())
                        || "ADMIN".equals(user.getRole())
        )) {

            // Always use the current database role
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + user.getRole()
                                    )
                            )
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            // Create a fresh security context
            var securityContext =
                    SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(
                    authentication
            );

            SecurityContextHolder.setContext(
                    securityContext
            );
        }

        filterChain.doFilter(request, response);
    }
}