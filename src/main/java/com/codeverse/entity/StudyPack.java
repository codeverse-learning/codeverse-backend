package com.codeverse.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "study_packs")
public class StudyPack {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;

    private String sourceType;

    private String sourceFileName;

    private String difficulty;

    @Builder.Default
    private String language = "English";

    private String summary;

    @Builder.Default
    private List<String> shortNotes = new ArrayList<>();

    @Builder.Default
    private List<KeyConcept> keyConcepts = new ArrayList<>();

    @Builder.Default
    private List<Flashcard> flashcards = new ArrayList<>();

    @Builder.Default
    private List<QuizQuestion> quizQuestions = new ArrayList<>();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    // Key Concept
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyConcept {

        private String term;

        private String definition;
    }

    // Flashcard
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Flashcard {

        private String question;

        private String answer;
    }

    // MCQ Question
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizQuestion {

        private String question;

        private List<String> options;

        private int correctAnswerIndex;

        private String explanation;
    }
}