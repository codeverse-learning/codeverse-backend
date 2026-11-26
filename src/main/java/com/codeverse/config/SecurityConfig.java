package com.codeverse.config;

import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${codeverse.auth.allowed-origin}")
    private String allowedOrigin;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        // Trusted frontend origin
        configuration.setAllowedOrigins(
                List.of(allowedOrigin)
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type"
                )
        );

        configuration.setExposedHeaders(
                List.of("Authorization")
        );

        // Required for Google authentication session cookie
        configuration.setAllowCredentials(true);

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // Authentication uses bearer tokens
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()
                ))

                // Do not create server-side login sessions
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Disable saved authentication requests
                .requestCache(cache -> cache.disable())

                // Return secure JSON authentication errors
                .exceptionHandling(exception -> exception

                        // Missing, invalid or expired authentication
                        .authenticationEntryPoint(
                                (request, response, authException) -> {

                                    response.setStatus(401);

                                    response.setContentType(
                                            MediaType.APPLICATION_JSON_VALUE
                                    );

                                    response.setCharacterEncoding(
                                            StandardCharsets.UTF_8.name()
                                    );

                                    response.setHeader(
                                            "Cache-Control",
                                            "no-store"
                                    );

                                    response.setHeader(
                                            "WWW-Authenticate",
                                            "Bearer"
                                    );

                                    response.getWriter().write(
                                            "{\"message\":\"Authentication required or session expired.\"}"
                                    );
                                }
                        )

                        // Authenticated user without permission
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> {

                                    response.setStatus(403);

                                    response.setContentType(
                                            MediaType.APPLICATION_JSON_VALUE
                                    );

                                    response.setCharacterEncoding(
                                            StandardCharsets.UTF_8.name()
                                    );

                                    response.setHeader(
                                            "Cache-Control",
                                            "no-store"
                                    );

                                    response.getWriter().write(
                                            "{\"message\":\"You do not have permission to access this resource.\"}"
                                    );
                                }
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()

                        // Browser preflight
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Public authentication endpoints
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/google",
                                "/api/auth/google/nonce",
                                "/api/auth/github"
                        ).permitAll()

                        // Public courses
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/courses",
                                "/api/courses/slug/*"
                        ).permitAll()

                        // Public modules
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/modules/course/*"
                        ).permitAll()

                        // Public published lessons
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/lessons/module/*"
                        ).permitAll()

                        // Admin course endpoints
                        .requestMatchers(
                                "/api/courses/admin/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/courses"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/courses/*"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/courses/*"
                        ).hasRole("ADMIN")

                        // Other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // JWT authentication filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // Disable default authentication methods
                .formLogin(form -> form.disable())

                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}