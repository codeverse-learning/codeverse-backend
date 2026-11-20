
package com.codeverse.service;

import com.codeverse.dto.request.CourseModuleRequest;
import com.codeverse.dto.response.CourseModuleResponse;
import com.codeverse.entity.Course;
import com.codeverse.entity.CourseModule;
import com.codeverse.repository.CourseRepository;
import com.codeverse.repository.CourseModuleRepository;
import com.codeverse.repository.LessonRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseModuleService {

    private final CourseModuleRepository moduleRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    // CREATE MODULE
    public CourseModuleResponse createModule(CourseModuleRequest request) {

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Course not found: " + request.getCourseId()
                ));

        CourseModule module = CourseModule.builder()
                .courseId(course.getId())
                .title(request.getTitle())
                .description(request.getDescription())
                .moduleOrder(request.getModuleOrder())
                .published(request.getPublished())
                .totalLessons(0)
                .createdAt(LocalDateTime.now())
                .build();

        CourseModule savedModule = moduleRepository.save(module);

        // UPDATE COURSE MODULE COUNT
        course.setTotalModules(
                (int) moduleRepository.countByCourseId(course.getId())
        );
        course.setUpdatedAt(LocalDateTime.now());
        courseRepository.save(course);

        return mapToResponse(savedModule);
    }

    // GET ALL MODULES BY COURSE
    public List<CourseModuleResponse> getModulesByCourse(String courseId) {

        return moduleRepository
                .findByCourseIdOrderByModuleOrderAsc(courseId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET PUBLISHED MODULES BY COURSE
    public List<CourseModuleResponse> getPublishedModules(String courseId) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Course not found: " + courseId
                ));

        if (!Boolean.TRUE.equals(course.getPublished())) {
            throw new IllegalArgumentException(
                    "Course is not published."
            );
        }

        return moduleRepository
                .findByCourseIdAndPublishedTrueOrderByModuleOrderAsc(courseId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET MODULE BY ID
    public CourseModuleResponse getModuleById(String id) {

        CourseModule module = moduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Module not found: " + id
                ));

        return mapToResponse(module);
    }

    // UPDATE MODULE
    public CourseModuleResponse updateModule(
            String id,
            CourseModuleRequest request
    ) {

        CourseModule module = moduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Module not found: " + id
                ));

        if (!module.getCourseId().equals(request.getCourseId())) {
            throw new IllegalArgumentException(
                    "Cannot move module to another course."
            );
        }

        module.setTitle(request.getTitle());
        module.setDescription(request.getDescription());
        module.setModuleOrder(request.getModuleOrder());
        module.setPublished(request.getPublished());
        module.setUpdatedAt(LocalDateTime.now());

        CourseModule updatedModule = moduleRepository.save(module);

        return mapToResponse(updatedModule);
    }

    // DELETE MODULE
    public void deleteModule(String id) {

        CourseModule module = moduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Module not found: " + id
                ));

        if (lessonRepository.countByModuleId(id) > 0) {
            throw new IllegalStateException(
                    "Delete module lessons before deleting this module."
            );
        }

        String courseId = module.getCourseId();

        moduleRepository.deleteById(id);

        courseRepository.findById(courseId).ifPresent(course -> {
            course.setTotalModules(
                    (int) moduleRepository.countByCourseId(courseId)
            );
            course.setUpdatedAt(LocalDateTime.now());
            courseRepository.save(course);
        });
    }

    // ENTITY TO RESPONSE
    private CourseModuleResponse mapToResponse(CourseModule module) {

        return CourseModuleResponse.builder()
                .id(module.getId())
                .courseId(module.getCourseId())
                .title(module.getTitle())
                .description(module.getDescription())
                .moduleOrder(module.getModuleOrder())
                .totalLessons(module.getTotalLessons())
                .published(module.getPublished())
                .createdAt(module.getCreatedAt())
                .updatedAt(module.getUpdatedAt())
                .build();
    }
}
