
package com.codeverse.repository;

import com.codeverse.entity.Course;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends MongoRepository<Course, String> {

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Course> findByPublishedTrue();

    List<Course> findByCategoryIgnoreCase(String category);

    List<Course> findByDifficultyLevelIgnoreCase(String difficultyLevel);
}
