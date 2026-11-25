package com.codeverse.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
public class AiLearningOutlineRequest {

    @NotBlank
    @Size(max = 80)
    private String programmingLanguage;

    @NotBlank
    @Size(max = 80)
    private String category = "Programming";

    @NotBlank
    @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED")
    private String difficultyLevel = "BEGINNER";

    @NotNull
    @Min(1)
    @Max(10)
    private Integer moduleCount = 5;

    @NotNull
    @Min(1)
    @Max(6)
    private Integer lessonsPerModule = 4;
}