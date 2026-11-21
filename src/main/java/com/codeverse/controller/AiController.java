
package com.codeverse.controller;

import com.codeverse.dto.request.CodeExplainRequest;
import com.codeverse.dto.response.CodeExplainResponse;
import com.codeverse.service.GeminiService;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final GeminiService geminiService;

    public AiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    // Existing AI Code Explanation
    @PostMapping("/explain")
    public ResponseEntity<CodeExplainResponse> explainCode(
            @Valid @RequestBody CodeExplainRequest request
    ) {

        String explanation = geminiService.explainCode(
                request.getLanguage(),
                request.getCode(),
                request.getExplanationLevel()
        );

        CodeExplainResponse response = CodeExplainResponse.builder()
                .language(request.getLanguage())
                .explanationLevel(request.getExplanationLevel())
                .explanation(explanation)
                .model(geminiService.getModel())
                .build();

        return ResponseEntity.ok(response);
    }

    // NEW: Interactive AI Learning Lesson
    @PostMapping("/interactive-lesson")
    public ResponseEntity<JsonNode> generateInteractiveLesson(
            @Valid @RequestBody CodeExplainRequest request
    ) {

        JsonNode lesson = geminiService.generateInteractiveLesson(
                request.getLanguage(),
                request.getCode(),
                request.getExplanationLevel()
        );

        return ResponseEntity.ok(lesson);
    }
}
