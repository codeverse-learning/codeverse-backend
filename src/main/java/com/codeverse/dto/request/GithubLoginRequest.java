package com.codeverse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class GithubLoginRequest {

    @NotBlank(
            message = "GitHub authorization code is required"
    )
    @Size(
            max = 2048,
            message = "Invalid GitHub authorization code length"
    )
    private String code;

    // PKCE Code Verifier
    @NotBlank(
            message = "GitHub PKCE code verifier is required"
    )
    @Size(
            min = 43,
            max = 128,
            message = "Invalid PKCE code verifier length"
    )
    @Pattern(
            regexp = "^[A-Za-z0-9._~-]+$",
            message = "Invalid PKCE code verifier format"
    )
    private String codeVerifier;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public void setCodeVerifier(String codeVerifier) {
        this.codeVerifier = codeVerifier;
    }
}