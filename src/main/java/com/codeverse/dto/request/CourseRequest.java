
package com.codeverse.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequest {

    private String title;

    private String slug;

    private String description;

    private String category;

    private String programmingLanguage;

    private String difficultyLevel;

    private String thumbnailUrl;

    private String instructorName;

    private Integer estimatedDurationHours;

    @Builder.Default
    private Boolean published = false;

    @Builder.Default
    private Boolean free = true;

    @Builder.Default
    private List<String> learningOutcomes = new ArrayList<>();

    @Builder.Default
    private List<String> prerequisites = new ArrayList<>();
}
