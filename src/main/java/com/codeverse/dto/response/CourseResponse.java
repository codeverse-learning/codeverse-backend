
package com.codeverse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {

    private String id;

    private String title;

    private String slug;

    private String description;

    private String category;

    private String programmingLanguage;

    private String difficultyLevel;

    private String thumbnailUrl;

    private String instructorName;

    private Integer estimatedDurationHours;

    private Boolean published;

    private Boolean free;

    @Builder.Default
    private List<String> learningOutcomes = new ArrayList<>();

    @Builder.Default
    private List<String> prerequisites = new ArrayList<>();

    private Integer totalModules;

    private Integer totalLessons;

    private Integer enrolledStudents;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
