package com.codeverse.service;

import com.codeverse.dto.request.StudyPackRequest;
import com.codeverse.dto.response.StudyPackResponse;
import com.codeverse.entity.StudyPack;
import com.codeverse.repository.StudyPackRepository;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudyPackService {

    private final StudyPackRepository studyPackRepository;
    private final GeminiService geminiService;
    private final DocumentTextExtractionService extractionService;

    private static final int MAX_CONTENT_LENGTH = 50000;

    private static final Set<String> DIFFICULTIES =
            Set.of("Beginner", "Intermediate", "Advanced");

    // Generate Study Pack from text or topic
    public StudyPackResponse generateFromContent(
            String userId,
            StudyPackRequest request
    ) {

        String sourceType = request.getSourceType();

        if (!"TEXT".equals(sourceType)
                && !"TOPIC".equals(sourceType)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Source type must be TEXT or TOPIC."
            );
        }

        return generateAndSave(
                userId,
                sourceType,
                request.getTitle(),
                request.getContent(),
                null,
                request.getDifficulty(),
                request.getLanguage()
        );
    }

    // Generate Study Pack from uploaded document
    public StudyPackResponse generateFromFile(
            String userId,
            MultipartFile file,
            String title,
            String difficulty,
            String language
    ) {

        String content;

        try {
            content = extractionService.extractText(file);

        } catch (IllegalArgumentException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }

        String fileName = file.getOriginalFilename();

        if (fileName != null) {
            fileName = fileName.replace('\\', '/');
            fileName = fileName.substring(
                    fileName.lastIndexOf('/') + 1
            );
        }

        return generateAndSave(
                userId,
                "FILE",
                title,
                content,
                fileName,
                difficulty,
                language
        );
    }

    // Shared generation and saving logic
    private StudyPackResponse generateAndSave(
            String userId,
            String sourceType,
            String title,
            String content,
            String sourceFileName,
            String difficulty,
            String language
    ) {

        validateUser(userId);

        if (content == null || content.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Learning material cannot be empty."
            );
        }

        content = content.strip();

        if (content.length() > MAX_CONTENT_LENGTH) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Content cannot exceed 50,000 characters."
            );
        }

        if (title != null && title.strip().length() > 200) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Title cannot exceed 200 characters."
            );
        }

        if (difficulty == null || difficulty.isBlank()) {
            difficulty = "Beginner";
        }

        if (!DIFFICULTIES.contains(difficulty)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid difficulty level."
            );
        }

        if (language == null || language.isBlank()) {
            language = "English";
        }

        if (!"English".equals(language)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only English is currently supported."
            );
        }

        // Generate Study Pack using Gemini AI
        JsonNode generated = geminiService.generateStudyPack(
                sourceType,
                content,
                difficulty,
                language
        );

        String finalTitle =
                title != null && !title.isBlank()
                        ? title.strip()
                        : generated.path("title").asText().strip();

        if (finalTitle.length() > 200) {
            finalTitle = finalTitle.substring(0, 200);
        }

        LocalDateTime now = LocalDateTime.now();

        StudyPack studyPack = StudyPack.builder()
                .userId(userId)
                .title(finalTitle)
                .sourceType(sourceType)
                .sourceFileName(sourceFileName)
                .difficulty(difficulty)
                .language(language)
                .summary(generated.path("summary").asText())
                .shortNotes(
                        convertTextList(
                                generated.path("shortNotes")
                        )
                )
                .keyConcepts(
                        convertKeyConcepts(
                                generated.path("keyConcepts")
                        )
                )
                .flashcards(
                        convertFlashcards(
                                generated.path("flashcards")
                        )
                )
                .quizQuestions(
                        convertQuizQuestions(
                                generated.path("quizQuestions")
                        )
                )
                .createdAt(now)
                .updatedAt(now)
                .build();

        StudyPack saved =
                studyPackRepository.save(studyPack);

        return mapToResponse(saved);
    }

    // Get all Study Packs belonging to the user
    public List<StudyPackResponse> getMyStudyPacks(
            String userId,
            String search
    ) {

        validateUser(userId);

        List<StudyPack> studyPacks;

        if (search == null || search.isBlank()) {

            studyPacks = studyPackRepository
                    .findByUserIdOrderByCreatedAtDesc(userId);

        } else {

            if (search.length() > 200) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Search text is too long."
                );
            }

            studyPacks = studyPackRepository
                    .findByUserIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                            userId,
                            search.strip()
                    );
        }

        return studyPacks.stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Get a single Study Pack
    public StudyPackResponse getStudyPack(
            String userId,
            String studyPackId
    ) {

        return mapToResponse(
                findOwnedStudyPack(userId, studyPackId)
        );
    }

    // Rename Study Pack
    public StudyPackResponse renameStudyPack(
            String userId,
            String studyPackId,
            String title
    ) {

        if (title == null || title.isBlank()
                || title.strip().length() > 200) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please provide a valid title."
            );
        }

        StudyPack studyPack =
                findOwnedStudyPack(userId, studyPackId);

        studyPack.setTitle(title.strip());
        studyPack.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(
                studyPackRepository.save(studyPack)
        );
    }

    // Delete Study Pack
    public void deleteStudyPack(
            String userId,
            String studyPackId
    ) {

        validateUser(userId);

        long deleted = studyPackRepository
                .deleteByIdAndUserId(studyPackId, userId);

        if (deleted == 0) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Study Pack not found."
            );
        }
    }

    // Count user's Study Packs
    public long countStudyPacks(String userId) {

        validateUser(userId);

        return studyPackRepository.countByUserId(userId);
    }

    // Validate authenticated user
    private void validateUser(String userId) {

        if (userId == null || userId.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required."
            );
        }
    }

    // Verify Study Pack ownership
    private StudyPack findOwnedStudyPack(
            String userId,
            String studyPackId
    ) {

        validateUser(userId);

        return studyPackRepository
                .findByIdAndUserId(studyPackId, userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Study Pack not found."
                        )
                );
    }

    // Convert JSON array into text list
    private List<String> convertTextList(JsonNode array) {

        List<String> result = new ArrayList<>();

        for (JsonNode item : array) {
            result.add(item.asText());
        }

        return result;
    }

    // Convert generated key concepts
    private List<StudyPack.KeyConcept> convertKeyConcepts(
            JsonNode array
    ) {

        List<StudyPack.KeyConcept> result =
                new ArrayList<>();

        for (JsonNode item : array) {

            result.add(
                    StudyPack.KeyConcept.builder()
                            .term(item.path("term").asText())
                            .definition(
                                    item.path("definition").asText()
                            )
                            .build()
            );
        }

        return result;
    }

    // Convert generated flashcards
    private List<StudyPack.Flashcard> convertFlashcards(
            JsonNode array
    ) {

        List<StudyPack.Flashcard> result =
                new ArrayList<>();

        for (JsonNode item : array) {

            result.add(
                    StudyPack.Flashcard.builder()
                            .question(
                                    item.path("question").asText()
                            )
                            .answer(
                                    item.path("answer").asText()
                            )
                            .build()
            );
        }

        return result;
    }

    // Convert generated MCQ questions
    private List<StudyPack.QuizQuestion> convertQuizQuestions(
            JsonNode array
    ) {

        List<StudyPack.QuizQuestion> result =
                new ArrayList<>();

        for (JsonNode item : array) {

            result.add(
                    StudyPack.QuizQuestion.builder()
                            .question(
                                    item.path("question").asText()
                            )
                            .options(
                                    convertTextList(
                                            item.path("options")
                                    )
                            )
                            .correctAnswerIndex(
                                    item.path("correctAnswerIndex").asInt()
                            )
                            .explanation(
                                    item.path("explanation").asText()
                            )
                            .build()
            );
        }

        return result;
    }

    // Convert entity into response DTO
    private StudyPackResponse mapToResponse(
            StudyPack studyPack
    ) {

        return StudyPackResponse.builder()
                .id(studyPack.getId())
                .title(studyPack.getTitle())
                .sourceType(studyPack.getSourceType())
                .sourceFileName(studyPack.getSourceFileName())
                .difficulty(studyPack.getDifficulty())
                .language(studyPack.getLanguage())
                .summary(studyPack.getSummary())
                .shortNotes(studyPack.getShortNotes())
                .keyConcepts(studyPack.getKeyConcepts())
                .flashcards(studyPack.getFlashcards())
                .quizQuestions(studyPack.getQuizQuestions())
                .createdAt(studyPack.getCreatedAt())
                .updatedAt(studyPack.getUpdatedAt())
                .build();
    }
}