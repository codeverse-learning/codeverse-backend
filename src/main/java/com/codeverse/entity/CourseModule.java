
package com.codeverse.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "course_modules")
public class CourseModule {

    @Id
    private String id;

    // Course ID that this module belongs to
    private String courseId;

    // Module details
    private String title;

    private String description;

    // Module order within the course
    private Integer moduleOrder;

    // Number of lessons inside this module
    @Builder.Default
    private Integer totalLessons = 0;

    // Module availability
    @Builder.Default
    private Boolean published = false;

    // Timestamps
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
