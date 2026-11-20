
package com.codeverse.service;

import com.codeverse.dto.request.CourseRequest;
import com.codeverse.dto.response.CourseResponse;
import com.codeverse.entity.Course;
import com.codeverse.repository.CourseRepository;
import com.codeverse.repository.CourseModuleRepository;
import com.codeverse.repository.LessonRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final LessonRepository lessonRepository;

    // CREATE COURSE
    public CourseResponse createCourse(CourseRequest request) {

        if (courseRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException(
                    "A course with this slug already exists."
            );
        }

        Course course = Course.builder()
                .title(request.getTitle())
                .slug(request.getSlug())
                .description(request.getDescription())
                .category(request.getCategory())
                .programmingLanguage(request.getProgrammingLanguage())
                .difficultyLevel(request.getDifficultyLevel())
                .thumbnailUrl(request.getThumbnailUrl())
                .instructorName(request.getInstructorName())
                .estimatedDurationHours(request.getEstimatedDurationHours())
                .published(request.getPublished())
                .free(request.getFree())
                .learningOutcomes(request.getLearningOutcomes())
                .prerequisites(request.getPrerequisites())
                .totalModules(0)
                .totalLessons(0)
                .enrolledStudents(0)
                .createdAt(LocalDateTime.now())
                .build();

        Course savedCourse = courseRepository.save(course);

        return mapToResponse(savedCourse);
    }

    // GET ALL COURSES
    public List<CourseResponse> getAllCourses() {

        return courseRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET PUBLISHED COURSES
    public List<CourseResponse> getPublishedCourses() {

        return courseRepository.findByPublishedTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET COURSE BY ID
    public CourseResponse getCourseById(String id) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Course not found with ID: " + id
                ));

        return mapToResponse(course);
    }

    // GET COURSE BY SLUG
    public CourseResponse getCourseBySlug(String slug) {

        Course course = courseRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Course not found with slug: " + slug
                ));

        return mapToResponse(course);
    }

    // UPDATE COURSE
    public CourseResponse updateCourse(
            String id,
            CourseRequest request
    ) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Course not found with ID: " + id
                ));

        if (!course.getSlug().equals(request.getSlug())
                && courseRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException(
                    "A course with this slug already exists."
            );
        }

        course.setTitle(request.getTitle());
        course.setSlug(request.getSlug());
        course.setDescription(request.getDescription());
        course.setCategory(request.getCategory());
        course.setProgrammingLanguage(request.getProgrammingLanguage());
        course.setDifficultyLevel(request.getDifficultyLevel());
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setInstructorName(request.getInstructorName());
        course.setEstimatedDurationHours(request.getEstimatedDurationHours());
        course.setPublished(request.getPublished());
        course.setFree(request.getFree());
        course.setLearningOutcomes(request.getLearningOutcomes());
        course.setPrerequisites(request.getPrerequisites());
        course.setUpdatedAt(LocalDateTime.now());

        Course updatedCourse = courseRepository.save(course);

        return mapToResponse(updatedCourse);
    }

    // DELETE COURSE
    public void deleteCourse(String id) {

        if (!courseRepository.existsById(id)) {
            throw new IllegalArgumentException(
                    "Course not found with ID: " + id
            );
        }

        // Avoid deleting a course that still has lessons or modules
        if (courseModuleRepository.countByCourseId(id) > 0
                || lessonRepository.countByCourseId(id) > 0) {
            throw new IllegalStateException(
                    "Delete course modules and lessons before deleting this course."
            );
        }

        courseRepository.deleteById(id);
    }

    // ENTITY TO RESPONSE DTO
    private CourseResponse mapToResponse(Course course) {

        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .slug(course.getSlug())
                .description(course.getDescription())
                .category(course.getCategory())
                .programmingLanguage(course.getProgrammingLanguage())
                .difficultyLevel(course.getDifficultyLevel())
                .thumbnailUrl(course.getThumbnailUrl())
                .instructorName(course.getInstructorName())
                .estimatedDurationHours(course.getEstimatedDurationHours())
                .published(course.getPublished())
                .free(course.getFree())
                .learningOutcomes(course.getLearningOutcomes())
                .prerequisites(course.getPrerequisites())
                .totalModules(course.getTotalModules())
                .totalLessons(course.getTotalLessons())
                .enrolledStudents(course.getEnrolledStudents())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
