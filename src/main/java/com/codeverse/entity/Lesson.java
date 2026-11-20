
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
@Document(collection = "lessons")
public class Lesson {

    @Id
    private String id;

    // Course and module relationships
    private String courseId;
    private String moduleId;

    // Lesson information
    private String title;
    private String description;

    // Lesson order inside module
    private Integer lessonOrder;

    // Lesson type: THEORY, PRACTICE, QUIZ, PROJECT
    @Builder.Default
    private String lessonType = "THEORY";

    // Main lesson content (Markdown format)
    private String content;

    // Programming code examples
    @Builder.Default
    private List<String> codeExamples = new ArrayList<>();

    // Practice exercises
    @Builder.Default
    private List<String> exercises = new ArrayList<>();

    // Optional external learning resources
    @Builder.Default
    private List<String> resourceLinks = new ArrayList<>();

    // Optional video tutorial URL
    private String videoUrl;

    // Estimated lesson duration in minutes
    private Integer estimatedDurationMinutes;

    // Lesson visibility
    @Builder.Default
    private Boolean published = false;

    // Free preview for non-enrolled students
    @Builder.Default
    private Boolean freePreview = false;

    // Timestamps
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
