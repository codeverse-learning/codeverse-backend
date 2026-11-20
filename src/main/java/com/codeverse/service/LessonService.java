
package com.codeverse.service;

import com.codeverse.dto.request.LessonRequest;
import com.codeverse.dto.response.LessonResponse;
import com.codeverse.entity.Course;
import com.codeverse.entity.CourseModule;
import com.codeverse.entity.Lesson;
import com.codeverse.repository.CourseRepository;
import com.codeverse.repository.CourseModuleRepository;
import com.codeverse.repository.LessonRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CourseModuleRepository moduleRepository;
    private final CourseRepository courseRepository;

    // CREATE LESSON
    public LessonResponse createLesson(LessonRequest request) {

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Course not found: " + request.getCourseId()
                ));

        CourseModule module = moduleRepository.findById(request.getModuleId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Module not found: " + request.getModuleId()
                ));

        if (!module.getCourseId().equals(course.getId())) {
            throw new IllegalArgumentException(
                    "Module does not belong to this course."
            );
        }

        Lesson lesson = Lesson.builder()
                .courseId(course.getId())
                .moduleId(module.getId())
                .title(request.getTitle())
                .description(request.getDescription())
                .lessonOrder(request.getLessonOrder())
                .lessonType(request.getLessonType())
                .content(request.getContent())
                .codeExamples(request.getCodeExamples())
                .exercises(request.getExercises())
                .resourceLinks(request.getResourceLinks())
                .videoUrl(request.getVideoUrl())
                .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                .published(request.getPublished())
                .freePreview(request.getFreePreview())
                .createdAt(LocalDateTime.now())
                .build();

        Lesson savedLesson = lessonRepository.save(lesson);

        updateLessonCounts(course, module);

        return mapToResponse(savedLesson);
    }

    // GET ALL LESSONS BY MODULE (ADMIN)
    public List<LessonResponse> getLessonsByModule(String moduleId) {

        return lessonRepository
                .findByModuleIdOrderByLessonOrderAsc(moduleId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET PUBLISHED LESSONS BY MODULE (STUDENT)
    public List<LessonResponse> getPublishedLessons(String moduleId) {

        CourseModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Module not found: " + moduleId
                ));

        Course course = courseRepository.findById(module.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Course not found."
                ));

        if (!Boolean.TRUE.equals(course.getPublished())
                || !Boolean.TRUE.equals(module.getPublished())) {

            throw new IllegalArgumentException(
                    "Course or module is not published."
            );
        }

        return lessonRepository
                .findByModuleIdAndPublishedTrueOrderByLessonOrderAsc(moduleId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET LESSON BY ID (ADMIN)
    public LessonResponse getLessonById(String id) {

        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lesson not found: " + id
                ));

        return mapToResponse(lesson);
    }

    // UPDATE LESSON
    public LessonResponse updateLesson(
            String id,
            LessonRequest request
    ) {

        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lesson not found: " + id
                ));

        if (!lesson.getCourseId().equals(request.getCourseId())
                || !lesson.getModuleId().equals(request.getModuleId())) {

            throw new IllegalArgumentException(
                    "Cannot move lesson to another course or module."
            );
        }

        lesson.setTitle(request.getTitle());
        lesson.setDescription(request.getDescription());
        lesson.setLessonOrder(request.getLessonOrder());
        lesson.setLessonType(request.getLessonType());
        lesson.setContent(request.getContent());
        lesson.setCodeExamples(request.getCodeExamples());
        lesson.setExercises(request.getExercises());
        lesson.setResourceLinks(request.getResourceLinks());
        lesson.setVideoUrl(request.getVideoUrl());
        lesson.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        lesson.setPublished(request.getPublished());
        lesson.setFreePreview(request.getFreePreview());
        lesson.setUpdatedAt(LocalDateTime.now());

        Lesson updatedLesson = lessonRepository.save(lesson);

        return mapToResponse(updatedLesson);
    }

    // DELETE LESSON
    public void deleteLesson(String id) {

        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lesson not found: " + id
                ));

        String courseId = lesson.getCourseId();
        String moduleId = lesson.getModuleId();

        lessonRepository.deleteById(id);

        Course course = courseRepository.findById(courseId)
                .orElse(null);

        CourseModule module = moduleRepository.findById(moduleId)
                .orElse(null);

        if (course != null && module != null) {
            updateLessonCounts(course, module);
        }
    }

    // UPDATE LESSON COUNTS
    private void updateLessonCounts(Course course, CourseModule module) {

        module.setTotalLessons(
                (int) lessonRepository.countByModuleId(module.getId())
        );

        module.setUpdatedAt(LocalDateTime.now());
        moduleRepository.save(module);

        course.setTotalLessons(
                (int) lessonRepository.countByCourseId(course.getId())
        );

        course.setUpdatedAt(LocalDateTime.now());
        courseRepository.save(course);
    }

    // ENTITY TO RESPONSE DTO
    private LessonResponse mapToResponse(Lesson lesson) {

        return LessonResponse.builder()
                .id(lesson.getId())
                .courseId(lesson.getCourseId())
                .moduleId(lesson.getModuleId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .lessonOrder(lesson.getLessonOrder())
                .lessonType(lesson.getLessonType())
                .content(lesson.getContent())
                .codeExamples(lesson.getCodeExamples())
                .exercises(lesson.getExercises())
                .resourceLinks(lesson.getResourceLinks())
                .videoUrl(lesson.getVideoUrl())
                .estimatedDurationMinutes(lesson.getEstimatedDurationMinutes())
                .published(lesson.getPublished())
                .freePreview(lesson.getFreePreview())
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }
}
