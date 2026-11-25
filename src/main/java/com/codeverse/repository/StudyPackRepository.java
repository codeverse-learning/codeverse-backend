package com.codeverse.repository;

import com.codeverse.entity.StudyPack;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyPackRepository
        extends MongoRepository<StudyPack, String> {

    // Get all study packs belonging to a user
    List<StudyPack> findByUserIdOrderByCreatedAtDesc(
            String userId
    );

    // Get a specific study pack belonging to a user
    Optional<StudyPack> findByIdAndUserId(
            String id,
            String userId
    );

    // Search study packs by title
    List<StudyPack> findByUserIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
            String userId,
            String title
    );

    // Count user's study packs
    long countByUserId(String userId);

    // Delete a study pack belonging to a user
    long deleteByIdAndUserId(
            String id,
            String userId
    );
}