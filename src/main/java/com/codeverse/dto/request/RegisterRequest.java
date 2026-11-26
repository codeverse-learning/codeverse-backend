package com.codeverse.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(
            min = 2,
            max = 100,
            message = "Full name must contain 2 to 100 characters"
    )
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email")
    @Size(
            max = 254,
            message = "Email is too long"
    )
    private String email;

    @NotBlank(message = "Password is required")
    @Size(
            min = 15,
            max = 72,
            message = "Password must contain 15 to 72 characters"
    )
    private String password;
}