package com.codeverse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.ToString;

@Data
public class GoogleLoginRequest {

    @NotBlank(message = "Google ID token is required")
    @Size(
            max = 16384,
            message = "Invalid Google ID token length"
    )
    @ToString.Exclude
    private String idToken;

    @NotBlank(message = "Google authentication nonce is required")
    @Size(
            min = 43,
            max = 43,
            message = "Invalid Google nonce length"
    )
    @Pattern(
            regexp = "^[A-Za-z0-9_-]+$",
            message = "Invalid Google nonce format"
    )
    @ToString.Exclude
    private String nonce;
}