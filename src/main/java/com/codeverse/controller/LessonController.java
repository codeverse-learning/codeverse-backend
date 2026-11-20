
package com.codeverse.controller;

import com.codeverse.dto.request.LessonRequest;
import com.codeverse.dto.response.LessonResponse;
import com.codeverse.service.LessonService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    // STUDENT: Get published lessons in a module
    @GetMapping("/module/{moduleId}")
    public ResponseEntity<List<LessonResponse>> getPublishedLessons(
            @PathVariable String moduleId
    ) {
        return ResponseEntity.ok(
                lessonService.getPublishedLessons(moduleId)
        );
    }

    // ADMIN: Get all lessons in a module
    @GetMapping("/admin/module/{moduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LessonResponse>> getAllLessons(
            @PathVariable String moduleId
    ) {
        return ResponseEntity.ok(
                lessonService.getLessonsByModule(moduleId)
        );
    }

    // ADMIN: Get lesson by ID
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LessonResponse> getLessonById(
            @PathVariable String id
    ) {
        return ResponseEntity.ok(
                lessonService.getLessonById(id)
        );
    }

    // ADMIN: Create lesson
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LessonResponse> createLesson(
            @RequestBody LessonRequest request
    ) {
        LessonResponse response =
                lessonService.createLesson(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ADMIN: Update lesson
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LessonResponse> updateLesson(
            @PathVariable String id,
            @RequestBody LessonRequest request
    ) {
        return ResponseEntity.ok(
                lessonService.updateLesson(id, request)
        );
    }

    // ADMIN: Delete lesson
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable String id
    ) {
        lessonService.deleteLesson(id);

        return ResponseEntity.noContent().build();
    }
}
