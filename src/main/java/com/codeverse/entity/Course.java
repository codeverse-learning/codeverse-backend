
package com.codeverse.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "courses")
public class Course {

    @Id
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

    @Builder.Default
    private Boolean published = false;

    @Builder.Default
    private Boolean free = true;

    @Builder.Default
    private List<String> learningOutcomes = new ArrayList<>();

    @Builder.Default
    private List<String> prerequisites = new ArrayList<>();

    @Builder.Default
    private Integer totalModules = 0;

    @Builder.Default
    private Integer totalLessons = 0;

    @Builder.Default
    private Integer enrolledStudents = 0;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
