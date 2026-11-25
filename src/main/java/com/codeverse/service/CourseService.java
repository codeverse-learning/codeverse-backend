
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

import com.codeverse.entity.CourseModule;
import com.codeverse.entity.Lesson;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;

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

    // PUBLISH FULL COURSE
    public synchronized CourseResponse publishFullCourse(String courseId) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course not found."
                ));

        List<CourseModule> modules = courseModuleRepository
                .findByCourseIdOrderByModuleOrderAsc(courseId);

        if (modules.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot publish a course without modules."
            );
        }

        List<Lesson> allLessons = new ArrayList<>();

        // Validate all content before publishing
        for (CourseModule module : modules) {

            List<Lesson> lessons = lessonRepository
                    .findByModuleIdOrderByLessonOrderAsc(module.getId());

            validateModuleLessons(module, lessons);

            allLessons.addAll(lessons);
        }

        if (allLessons.size() != lessonRepository.countByCourseId(courseId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Course contains inconsistent lesson relationships."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        // Publish lessons first
        for (Lesson lesson : allLessons) {
            if (!Boolean.TRUE.equals(lesson.getPublished())) {
                lesson.setPublished(true);
                lesson.setUpdatedAt(now);
                lessonRepository.save(lesson);
            }
        }

        // Publish modules
        for (CourseModule module : modules) {
            if (!Boolean.TRUE.equals(module.getPublished())) {
                module.setPublished(true);
                module.setUpdatedAt(now);
                courseModuleRepository.save(module);
            }
        }

        // Publish course last
        course.setPublished(true);
        course.setUpdatedAt(now);

        return mapToResponse(courseRepository.save(course));
    }


    // UNPUBLISH FULL COURSE
    public synchronized CourseResponse unpublishFullCourse(String courseId) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course not found."
                ));

        LocalDateTime now = LocalDateTime.now();

        // Hide course first
        course.setPublished(false);
        course.setUpdatedAt(now);

        courseRepository.save(course);

        List<CourseModule> modules = courseModuleRepository
                .findByCourseIdOrderByModuleOrderAsc(courseId);

        for (CourseModule module : modules) {

            if (Boolean.TRUE.equals(module.getPublished())) {
                module.setPublished(false);
                module.setUpdatedAt(now);
                courseModuleRepository.save(module);
            }

            List<Lesson> lessons = lessonRepository
                    .findByModuleIdOrderByLessonOrderAsc(module.getId());

            for (Lesson lesson : lessons) {
                if (Boolean.TRUE.equals(lesson.getPublished())) {
                    lesson.setPublished(false);
                    lesson.setUpdatedAt(now);
                    lessonRepository.save(lesson);
                }
            }
        }

        return mapToResponse(course);
    }


    // PUBLISH ENTIRE MODULE
    public synchronized CourseResponse publishEntireModule(
            String courseId,
            String moduleId
    ) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course not found."
                ));

        CourseModule module = findCourseModule(courseId, moduleId);

        List<Lesson> lessons = lessonRepository
                .findByModuleIdOrderByLessonOrderAsc(moduleId);

        validateModuleLessons(module, lessons);

        LocalDateTime now = LocalDateTime.now();

        for (Lesson lesson : lessons) {
            if (!Boolean.TRUE.equals(lesson.getPublished())) {
                lesson.setPublished(true);
                lesson.setUpdatedAt(now);
                lessonRepository.save(lesson);
            }
        }

        module.setPublished(true);
        module.setUpdatedAt(now);

        courseModuleRepository.save(module);

        return mapToResponse(course);
    }


    // UNPUBLISH ENTIRE MODULE
    public synchronized CourseResponse unpublishEntireModule(
            String courseId,
            String moduleId
    ) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course not found."
                ));

        CourseModule module = findCourseModule(courseId, moduleId);

        LocalDateTime now = LocalDateTime.now();

        module.setPublished(false);
        module.setUpdatedAt(now);

        courseModuleRepository.save(module);

        List<Lesson> lessons = lessonRepository
                .findByModuleIdOrderByLessonOrderAsc(moduleId);

        for (Lesson lesson : lessons) {
            if (Boolean.TRUE.equals(lesson.getPublished())) {
                lesson.setPublished(false);
                lesson.setUpdatedAt(now);
                lessonRepository.save(lesson);
            }
        }

        return mapToResponse(course);
    }


    // VALIDATE COURSE AND MODULE RELATIONSHIP
    private CourseModule findCourseModule(
            String courseId,
            String moduleId
    ) {

        CourseModule module = courseModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Module not found."
                ));

        if (!courseId.equals(module.getCourseId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Module does not belong to this course."
            );
        }

        return module;
    }


    // VALIDATE LESSON CONTENT BEFORE PUBLISHING
    private void validateModuleLessons(
            CourseModule module,
            List<Lesson> lessons
    ) {

        if (lessons.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Module has no lessons: " + module.getTitle()
            );
        }

        for (Lesson lesson : lessons) {

            if (!module.getCourseId().equals(lesson.getCourseId())
                    || !module.getId().equals(lesson.getModuleId())) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Invalid lesson relationship: " + lesson.getTitle()
                );
            }

            if (lesson.getContent() == null
                    || lesson.getContent().isBlank()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Lesson content is missing: " + lesson.getTitle()
                );
            }
        }
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
