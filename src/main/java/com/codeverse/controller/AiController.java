package com.codeverse.controller;

import com.codeverse.dto.request.CodeExplainRequest;
import com.codeverse.dto.response.CodeExplainResponse;
import com.codeverse.service.GeminiService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final GeminiService geminiService;

    public AiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/explain")
    public ResponseEntity<CodeExplainResponse> explainCode(
            @Valid @RequestBody CodeExplainRequest request) {
        String explanation = geminiService.explainCode(
                request.getLanguage(), request.getCode(), request.getExplanationLevel());
        return ResponseEntity.ok(CodeExplainResponse.builder()
                .language(request.getLanguage())
                .explanationLevel(request.getExplanationLevel())
                .explanation(explanation)
                .model(geminiService.getModel())
                .build());
    }

    @PostMapping(value = "/interactive-lesson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> generateInteractiveLesson(
            @Valid @RequestBody CodeExplainRequest request) {
        JsonNode lesson = geminiService.generateInteractiveLesson(
                request.getLanguage(), request.getCode(), request.getExplanationLevel());
        return ResponseEntity.ok(lesson.toString());
    }

    @PostMapping(value = "/visual-lesson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> generateVisualLesson(
            @Valid @RequestBody CodeExplainRequest request) {
        JsonNode lesson = geminiService.generateVisualLesson(
                request.getLanguage(), request.getCode(), request.getExplanationLevel(), "en");
        return ResponseEntity.ok(lesson.toString());
    }

    // Returns chapter ranges without making a Gemini API call.
    @PostMapping(value = "/visual-chapters/outline", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> visualChapterOutline(
            @Valid @RequestBody CodeExplainRequest request) {
        return ResponseEntity.ok(geminiService.getVisualChapterOutline(request.getCode()));
    }

    // Generate only one chapter at a time. chapterIndex is zero-based.
    @PostMapping(value = "/visual-chapters/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> generateVisualChapter(
            @Valid @RequestBody CodeExplainRequest request,
            @RequestParam int chapterIndex) {
        return ResponseEntity.ok(geminiService.generateVisualChapter(
                request.getLanguage(), request.getCode(), request.getExplanationLevel(), chapterIndex));
    }
}
