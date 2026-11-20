
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
public class LessonRequest {

    private String courseId;

    private String moduleId;

    private String title;

    private String description;

    private Integer lessonOrder;

    @Builder.Default
    private String lessonType = "THEORY";

    private String content;

    @Builder.Default
    private List<String> codeExamples = new ArrayList<>();

    @Builder.Default
    private List<String> exercises = new ArrayList<>();

    @Builder.Default
    private List<String> resourceLinks = new ArrayList<>();

    private String videoUrl;

    private Integer estimatedDurationMinutes;

    @Builder.Default
    private Boolean published = false;

    @Builder.Default
    private Boolean freePreview = false;
}
