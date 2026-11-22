
package com.codeverse.service;

import com.codeverse.entity.AiLearningHistory;
import com.codeverse.repository.AiLearningHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiLearningHistoryService {

    private final AiLearningHistoryRepository historyRepository;

    // Save AI learning history
    public AiLearningHistory saveHistory(
            String userId,
            String language,
            String code,
            String explanationLevel,
            String learningMode,
            String title,
            String result
    ) {
        AiLearningHistory history = AiLearningHistory.builder()
                .userId(userId)
                .language(language)
                .code(code)
                .explanationLevel(explanationLevel)
                .learningMode(learningMode)
                .title(title)
                .result(result)
                .createdAt(LocalDateTime.now())
                .build();

        return historyRepository.save(history);
    }

    // Get all history records for a user
    public List<AiLearningHistory> getUserHistory(String userId) {
        return historyRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Get a specific history record
    public AiLearningHistory getHistoryById(
            String historyId,
            String userId
    ) {
        return historyRepository
                .findByIdAndUserId(historyId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "History record not found."
                ));
    }

    // Delete a user's history record
    public void deleteHistory(
            String historyId,
            String userId
    ) {
        AiLearningHistory history = getHistoryById(
                historyId,
                userId
        );

        historyRepository.delete(history);
    }
}
