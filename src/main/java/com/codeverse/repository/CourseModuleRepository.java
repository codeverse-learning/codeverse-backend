
package com.codeverse.repository;

import com.codeverse.entity.CourseModule;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CourseModuleRepository
        extends MongoRepository<CourseModule, String> {

    List<CourseModule> findByCourseIdOrderByModuleOrderAsc(String courseId);

    List<CourseModule> findByCourseIdAndPublishedTrueOrderByModuleOrderAsc(
            String courseId
    );

    long countByCourseId(String courseId);
}
