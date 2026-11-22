
package com.codeverse.repository;

import com.codeverse.entity.AiLearningHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AiLearningHistoryRepository
        extends MongoRepository<AiLearningHistory, String> {

    // Get a user's history, newest first
    List<AiLearningHistory> findByUserIdOrderByCreatedAtDesc(
            String userId
    );

    // Get one history record belonging to the user
    Optional<AiLearningHistory> findByIdAndUserId(
            String id,
            String userId
    );

    // Delete only records belonging to the user
    long deleteByIdAndUserId(
            String id,
            String userId
    );
}
