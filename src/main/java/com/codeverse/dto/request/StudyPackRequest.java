package com.codeverse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyPackRequest {

    @NotBlank(message = "Source type is required")
    @Pattern(
            regexp = "FILE|TEXT|TOPIC",
            message = "Invalid source type"
    )
    private String sourceType;

    @Size(
            max = 200,
            message = "Title cannot exceed 200 characters"
    )
    private String title;

    @Size(
            max = 50000,
            message = "Content cannot exceed 50000 characters"
    )
    private String content;

    @Builder.Default
    @NotBlank
    @Pattern(
            regexp = "Beginner|Intermediate|Advanced",
            message = "Invalid difficulty level"
    )
    private String difficulty = "Beginner";

    @Builder.Default
    @NotBlank
    @Pattern(
            regexp = "English",
            message = "Only English is currently supported"
    )
    private String language = "English";
}