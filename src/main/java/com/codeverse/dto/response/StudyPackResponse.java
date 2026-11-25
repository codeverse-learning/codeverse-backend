package com.codeverse.dto.response;

import com.codeverse.entity.StudyPack;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyPackResponse {

    private String id;

    private String title;

    private String sourceType;

    private String sourceFileName;

    private String difficulty;

    private String language;

    private String summary;

    private List<String> shortNotes;

    private List<StudyPack.KeyConcept> keyConcepts;

    private List<StudyPack.Flashcard> flashcards;

    private List<StudyPack.QuizQuestion> quizQuestions;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}