
package com.codeverse.controller;

import com.codeverse.dto.request.CourseModuleRequest;
import com.codeverse.dto.response.CourseModuleResponse;
import com.codeverse.service.CourseModuleService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
public class CourseModuleController {

    private final CourseModuleService moduleService;

    // PUBLIC: Get published modules of a course
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<CourseModuleResponse>> getPublishedModules(
            @PathVariable String courseId
    ) {
        return ResponseEntity.ok(
                moduleService.getPublishedModules(courseId)
        );
    }

    // ADMIN: Get all modules of a course
    @GetMapping("/admin/course/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CourseModuleResponse>> getAllModules(
            @PathVariable String courseId
    ) {
        return ResponseEntity.ok(
                moduleService.getModulesByCourse(courseId)
        );
    }

    // ADMIN: Get module by ID
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseModuleResponse> getModuleById(
            @PathVariable String id
    ) {
        return ResponseEntity.ok(
                moduleService.getModuleById(id)
        );
    }

    // ADMIN: Create module
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseModuleResponse> createModule(
            @RequestBody CourseModuleRequest request
    ) {
        CourseModuleResponse response =
                moduleService.createModule(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ADMIN: Update module
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseModuleResponse> updateModule(
            @PathVariable String id,
            @RequestBody CourseModuleRequest request
    ) {
        return ResponseEntity.ok(
                moduleService.updateModule(id, request)
        );
    }

    // ADMIN: Delete module
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteModule(
            @PathVariable String id
    ) {
        moduleService.deleteModule(id);
        return ResponseEntity.noContent().build();
    }
}
