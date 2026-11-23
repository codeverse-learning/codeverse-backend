
package com.codeverse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodeTraceRequest(

        @NotBlank(message = "Programming language is required")
        String language,

        @NotBlank(message = "Code cannot be empty")
        @Size(max = 10000, message = "Code is too long")
        String code

) {}
