package com.codeverse.service;

import com.codeverse.dto.request.AiLearningOutlineRequest;
import com.codeverse.dto.request.CourseRequest;
import com.codeverse.dto.request.CourseModuleRequest;
import com.codeverse.dto.request.LessonRequest;

import com.codeverse.dto.response.CourseResponse;
import com.codeverse.dto.response.CourseModuleResponse;

import com.codeverse.repository.CourseRepository;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.codeverse.dto.response.LessonResponse;
import com.codeverse.entity.Course;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AiLearningContentService {

    private final GeminiService geminiService;
    private final CourseService courseService;
    private final CourseModuleService courseModuleService;
    private final LessonService lessonService;
    private final CourseRepository courseRepository;

    // GENERATE AND SAVE LEARNING OUTLINE
    public synchronized CourseResponse generateOutline(
            AiLearningOutlineRequest request
    ) {

        String language = request.getProgrammingLanguage().trim();
        String difficulty = request.getDifficultyLevel();

        String slug = generateSlug(
                language + "-" + difficulty
        );

        if (slug.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid programming language."
            );
        }

        // PREVENT DUPLICATE LEARNING MATERIALS
        if (courseRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Learning material already exists for this language and level."
            );
        }

        // GENERATE OUTLINE USING GEMINI
        JsonNode outline = geminiService.generateLearningOutline(
                language,
                difficulty,
                request.getModuleCount(),
                request.getLessonsPerModule()
        );

        // VALIDATE GENERATED FIELD LENGTHS BEFORE SAVING
        validateText(outline, "title", 200);
        validateText(outline, "description", 2000);

        for (JsonNode module : outline.path("modules")) {

            validateText(module, "title", 200);
            validateText(module, "description", 2000);

            for (JsonNode lesson : module.path("lessons")) {

                validateText(lesson, "title", 200);
                validateText(lesson, "description", 2000);
            }
        }

        int totalLessons =
                request.getModuleCount()
                        * request.getLessonsPerModule();

        int estimatedHours = Math.max(
                1,
                (int) Math.ceil(totalLessons * 20.0 / 60)
        );

        // CREATE DRAFT COURSE
        CourseRequest courseRequest = CourseRequest.builder()
                .title(outline.path("title").asText())
                .slug(slug)
                .description(outline.path("description").asText())
                .category(request.getCategory().trim())
                .programmingLanguage(language)
                .difficultyLevel(difficulty)
                .instructorName("CODEVERSE Team")
                .estimatedDurationHours(estimatedHours)
                .published(false)
                .free(true)
                .learningOutcomes(
                        toStringList(outline.path("learningOutcomes"))
                )
                .prerequisites(
                        toStringList(outline.path("prerequisites"))
                )
                .build();

        CourseResponse course =
                courseService.createCourse(courseRequest);

        int moduleOrder = 1;

        // CREATE DRAFT MODULES
        for (JsonNode module : outline.path("modules")) {

            CourseModuleRequest moduleRequest =
                    CourseModuleRequest.builder()
                            .courseId(course.getId())
                            .title(module.path("title").asText())
                            .description(module.path("description").asText())
                            .moduleOrder(moduleOrder++)
                            .published(false)
                            .build();

            CourseModuleResponse savedModule =
                    courseModuleService.createModule(moduleRequest);

            int lessonOrder = 1;

            // CREATE DRAFT LESSON OUTLINES
            for (JsonNode lesson : module.path("lessons")) {

                LessonRequest lessonRequest =
                        LessonRequest.builder()
                                .courseId(course.getId())
                                .moduleId(savedModule.getId())
                                .title(lesson.path("title").asText())
                                .description(
                                        lesson.path("description").asText()
                                )
                                .lessonOrder(lessonOrder++)
                                .lessonType("THEORY")
                                .content("")
                                .codeExamples(new ArrayList<>())
                                .exercises(new ArrayList<>())
                                .resourceLinks(new ArrayList<>())
                                .estimatedDurationMinutes(20)
                                .published(false)
                                .freePreview(false)
                                .build();

                lessonService.createLesson(lessonRequest);
            }
        }

        // RETURN UPDATED COURSE WITH COUNTS
        return courseService.getCourseById(course.getId());
    }

    // GENERATE DETAILED CONTENT FOR AN EXISTING DRAFT LESSON
    public synchronized LessonResponse generateLessonContent(
            String lessonId
    ) {

        // GET EXISTING LESSON
        LessonResponse lesson =
                lessonService.getLessonById(lessonId);

        // GET EXISTING MODULE
        CourseModuleResponse module =
                courseModuleService.getModuleById(
                        lesson.getModuleId()
                );

        // GET EXISTING COURSE
        Course course = courseRepository
                .findById(lesson.getCourseId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Course not found."
                ));

        // VALIDATE COURSE AND MODULE RELATIONSHIP
        if (!course.getId().equals(module.getCourseId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid course and module relationship."
            );
        }

        // PROTECT PUBLISHED CONTENT
        if (Boolean.TRUE.equals(lesson.getPublished())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot generate content for a published lesson."
            );
        }

        // PREVENT OVERWRITING EXISTING CONTENT
        if ((lesson.getContent() != null
                && !lesson.getContent().isBlank())
                || (lesson.getCodeExamples() != null
                && !lesson.getCodeExamples().isEmpty())
                || (lesson.getExercises() != null
                && !lesson.getExercises().isEmpty())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This lesson already contains learning content."
            );
        }

        // GENERATE CONTENT USING GEMINI
        JsonNode generatedLesson =
                geminiService.generateLearningLesson(
                        course.getProgrammingLanguage(),
                        course.getDifficultyLevel(),
                        module.getTitle(),
                        lesson.getTitle(),
                        lesson.getDescription() == null
                                ? ""
                                : lesson.getDescription()
                );

        // VALIDATE GENERATED CONTENT
        validateText(
                generatedLesson,
                "description",
                2000
        );

        validateText(
                generatedLesson,
                "content",
                50000
        );

        // UPDATE EXISTING DRAFT LESSON
        LessonRequest request = LessonRequest.builder()

                .courseId(lesson.getCourseId())
                .moduleId(lesson.getModuleId())
                .title(lesson.getTitle())

                .description(
                        generatedLesson.path("description").asText()
                )

                .lessonOrder(lesson.getLessonOrder())

                .lessonType(
                        generatedLesson.path("lessonType").asText()
                )

                .content(
                        generatedLesson.path("content").asText()
                )

                .codeExamples(
                        toStringList(
                                generatedLesson.path("codeExamples")
                        )
                )

                .exercises(
                        toStringList(
                                generatedLesson.path("exercises")
                        )
                )

                .resourceLinks(
                        lesson.getResourceLinks() == null
                                ? new ArrayList<>()
                                : lesson.getResourceLinks()
                )

                .videoUrl(lesson.getVideoUrl())

                .estimatedDurationMinutes(
                        generatedLesson
                                .path("estimatedDurationMinutes")
                                .asInt()
                )

                .published(false)
                .freePreview(lesson.getFreePreview())
                .build();

        return lessonService.updateLesson(
                lessonId,
                request
        );
    }

    // GENERATE URL-FRIENDLY SLUG
    private String generateSlug(String value) {

        return value.toLowerCase(Locale.ROOT)
                .replace("++", " plus plus ")
                .replace("#", " sharp ")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    // CONVERT JSON ARRAY TO STRING LIST
    private List<String> toStringList(JsonNode array) {

        List<String> values = new ArrayList<>();

        if (array.isArray()) {

            for (JsonNode item : array) {

                if (item.isTextual()
                        && !item.asText().isBlank()) {

                    values.add(item.asText().trim());
                }
            }
        }

        return values;
    }

    // VALIDATE AI GENERATED TEXT
    private void validateText(
            JsonNode node,
            String field,
            int maxLength
    ) {

        JsonNode value = node.path(field);

        if (!value.isTextual()
                || value.asText().isBlank()
                || value.asText().length() > maxLength) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Invalid generated field: " + field
            );
        }
    }
}