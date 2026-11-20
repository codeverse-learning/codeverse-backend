
package com.codeverse.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseModuleRequest {

    private String courseId;

    private String title;

    private String description;

    private Integer moduleOrder;

    @Builder.Default
    private Boolean published = false;
}
