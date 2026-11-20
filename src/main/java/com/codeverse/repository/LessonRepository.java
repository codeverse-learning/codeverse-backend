
package com.codeverse.repository;

import com.codeverse.entity.Lesson;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LessonRepository extends MongoRepository<Lesson, String> {

    List<Lesson> findByModuleIdOrderByLessonOrderAsc(String moduleId);

    List<Lesson> findByModuleIdAndPublishedTrueOrderByLessonOrderAsc(
            String moduleId
    );

    List<Lesson> findByCourseId(String courseId);

    long countByModuleId(String moduleId);

    long countByCourseId(String courseId);
}
