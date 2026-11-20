
package com.codeverse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseModuleResponse {

    private String id;

    private String courseId;

    private String title;

    private String description;

    private Integer moduleOrder;

    private Integer totalLessons;

    private Boolean published;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
