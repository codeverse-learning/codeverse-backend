
package com.codeverse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CodeExplainRequest {

    @NotBlank(message = "Programming language is required")
    private String language;

    @NotBlank(message = "Code cannot be empty")
    @Size(max = 20000, message = "Code must not exceed 20000 characters")
    private String code;

    @NotBlank(message = "Explanation level is required")
    private String explanationLevel;
}
