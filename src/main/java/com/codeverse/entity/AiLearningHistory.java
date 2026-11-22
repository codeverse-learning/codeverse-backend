
package com.codeverse.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ai_learning_history")
public class AiLearningHistory {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String language;

    private String code;

    private String explanationLevel;

    // EXPLANATION, INTERACTIVE_LESSON, VISUAL_LESSON
    private String learningMode;

    private String title;

    // Stores the generated Markdown or JSON
    private String result;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
