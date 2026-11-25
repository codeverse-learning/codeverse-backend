
package com.codeverse.controller;

import com.codeverse.dto.request.CourseRequest;
import com.codeverse.dto.response.CourseResponse;
import com.codeverse.service.CourseService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.codeverse.dto.request.AiLearningOutlineRequest;
import com.codeverse.service.AiLearningContentService;
import jakarta.validation.Valid;

import com.codeverse.dto.response.LessonResponse;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final AiLearningContentService aiLearningContentService;

    // PUBLIC: Get published courses
    @GetMapping
    public ResponseEntity<List<CourseResponse>> getPublishedCourses() {
        return ResponseEntity.ok(
                courseService.getPublishedCourses()
        );
    }

    // ADMIN: Get all courses, including drafts
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CourseResponse>> getAllCourses() {
        return ResponseEntity.ok(
                courseService.getAllCourses()
        );
    }

    // PUBLIC: Get published course by slug
    @GetMapping("/slug/{slug}")
    public ResponseEntity<CourseResponse> getCourseBySlug(
            @PathVariable String slug
    ) {
        CourseResponse course = courseService.getCourseBySlug(slug);

        if (!Boolean.TRUE.equals(course.getPublished())) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(course);
    }

    // ADMIN: Get course by ID
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponse> getCourseById(
            @PathVariable String id
    ) {
        return ResponseEntity.ok(
                courseService.getCourseById(id)
        );
    }

    // ADMIN: Create course
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponse> createCourse(
            @RequestBody CourseRequest request
    ) {
        CourseResponse response = courseService.createCourse(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ADMIN: Update course
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable String id,
            @RequestBody CourseRequest request
    ) {
        return ResponseEntity.ok(
                courseService.updateCourse(id, request)
        );
    }

    // ADMIN: Delete course
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable String id
    ) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    // ADMIN: Generate AI Learning Outline
    @PostMapping("/admin/generate-outline")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponse> generateLearningOutline(
            @Valid @RequestBody AiLearningOutlineRequest request
    ) {

        CourseResponse response =
                aiLearningContentService.generateOutline(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ADMIN: Generate detailed AI lesson content
    @PostMapping("/admin/generate-lesson/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LessonResponse> generateLessonContent(
            @PathVariable String lessonId
    ) {

        return ResponseEntity.ok(
                aiLearningContentService
                        .generateLessonContent(lessonId)
        );
    }

    // ADMIN: Publish full course
    @PatchMapping("/admin/{courseId}/publish-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponse> publishFullCourse(
            @PathVariable String courseId
    ) {
        return ResponseEntity.ok(
                courseService.publishFullCourse(courseId)
        );
    }


    // ADMIN: Unpublish full course
    @PatchMapping("/admin/{courseId}/unpublish-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponse> unpublishFullCourse(
            @PathVariable String courseId
    ) {
        return ResponseEntity.ok(
                courseService.unpublishFullCourse(courseId)
        );
    }


    // ADMIN: Publish entire module
    @PatchMapping("/admin/{courseId}/modules/{moduleId}/publish-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponse> publishEntireModule(
            @PathVariable String courseId,
            @PathVariable String moduleId
    ) {
        return ResponseEntity.ok(
                courseService.publishEntireModule(courseId, moduleId)
        );
    }


    // ADMIN: Unpublish entire module
    @PatchMapping("/admin/{courseId}/modules/{moduleId}/unpublish-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponse> unpublishEntireModule(
            @PathVariable String courseId,
            @PathVariable String moduleId
    ) {
        return ResponseEntity.ok(
                courseService.unpublishEntireModule(courseId, moduleId)
        );
    }
}
