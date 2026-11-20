
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
public class LessonResponse {

    private String id;

    private String courseId;

    private String moduleId;

    private String title;

    private String description;

    private Integer lessonOrder;

    private String lessonType;

    private String content;

    @Builder.Default
    private List<String> codeExamples = new ArrayList<>();

    @Builder.Default
    private List<String> exercises = new ArrayList<>();

    @Builder.Default
    private List<String> resourceLinks = new ArrayList<>();

    private String videoUrl;

    private Integer estimatedDurationMinutes;

    private Boolean published;

    private Boolean freePreview;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
