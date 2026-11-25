package com.codeverse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudyPackRenameRequest {

    @NotBlank(message = "Title is required")
    @Size(
            max = 200,
            message = "Title cannot exceed 200 characters"
    )
    private String title;
}