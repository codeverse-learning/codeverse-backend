
package com.codeverse.controller;

import com.codeverse.dto.request.CodeExplainRequest;
import com.codeverse.dto.response.CodeExplainResponse;
import com.codeverse.entity.User;
import com.codeverse.service.AiLearningHistoryService;
import com.codeverse.service.GeminiService;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final GeminiService geminiService;
    private final AiLearningHistoryService historyService;

    public AiController(
            GeminiService geminiService,
            AiLearningHistoryService historyService
    ) {
        this.geminiService = geminiService;
        this.historyService = historyService;
    }

    @PostMapping("/explain")
    public ResponseEntity<CodeExplainResponse> explainCode(
            @Valid @RequestBody CodeExplainRequest request,
            @AuthenticationPrincipal User user
    ) {
        String explanation = geminiService.explainCode(
                request.getLanguage(),
                request.getCode(),
                request.getExplanationLevel()
        );

        historyService.saveHistory(
                user.getId(),
                request.getLanguage(),
                request.getCode(),
                request.getExplanationLevel(),
                "EXPLANATION",
                request.getLanguage() + " Code Explanation",
                explanation
        );

        return ResponseEntity.ok(
                CodeExplainResponse.builder()
                        .language(request.getLanguage())
                        .explanationLevel(request.getExplanationLevel())
                        .explanation(explanation)
                        .model(geminiService.getModel())
                        .build()
        );
    }

    @PostMapping(
            value = "/interactive-lesson",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> generateInteractiveLesson(
            @Valid @RequestBody CodeExplainRequest request,
            @AuthenticationPrincipal User user
    ) {
        JsonNode lesson = geminiService.generateInteractiveLesson(
                request.getLanguage(),
                request.getCode(),
                request.getExplanationLevel()
        );

        historyService.saveHistory(
                user.getId(),
                request.getLanguage(),
                request.getCode(),
                request.getExplanationLevel(),
                "INTERACTIVE_LESSON",
                lesson.path("title").asText("Interactive Lesson"),
                lesson.toString()
        );

        return ResponseEntity.ok(lesson.toString());
    }

    @PostMapping(
            value = "/visual-lesson",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> generateVisualLesson(
            @Valid @RequestBody CodeExplainRequest request,
            @AuthenticationPrincipal User user
    ) {
        JsonNode lesson = geminiService.generateVisualLesson(
                request.getLanguage(),
                request.getCode(),
                request.getExplanationLevel(),
                "en"
        );

        historyService.saveHistory(
                user.getId(),
                request.getLanguage(),
                request.getCode(),
                request.getExplanationLevel(),
                "VISUAL_LESSON",
                lesson.path("title").asText("Visual Learning"),
                lesson.toString()
        );

        return ResponseEntity.ok(lesson.toString());
    }

    @PostMapping(
            value = "/visual-chapters/outline",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<JsonNode> visualChapterOutline(
            @Valid @RequestBody CodeExplainRequest request
    ) {
        return ResponseEntity.ok(
                geminiService.getVisualChapterOutline(request.getCode())
        );
    }

    @PostMapping(
            value = "/visual-chapters/generate",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<JsonNode> generateVisualChapter(
            @Valid @RequestBody CodeExplainRequest request,
            @RequestParam int chapterIndex,
            @AuthenticationPrincipal User user
    ) {
        JsonNode chapter = geminiService.generateVisualChapter(
                request.getLanguage(),
                request.getCode(),
                request.getExplanationLevel(),
                chapterIndex
        );

        historyService.saveHistory(
                user.getId(),
                request.getLanguage(),
                request.getCode(),
                request.getExplanationLevel(),
                "VISUAL_LESSON",
                "Visual Learning - Chapter " + (chapterIndex + 1),
                chapter.toString()
        );

        return ResponseEntity.ok(chapter);
    }
}
