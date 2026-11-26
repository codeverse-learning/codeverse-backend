package com.codeverse.config;

import com.codeverse.entity.User;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;

import org.bson.Document;

@Configuration
public class MongoIndexConfig {

    @Bean
    public ApplicationRunner createUserIndexes(
            MongoTemplate mongoTemplate) {

        return args -> {

            // Google ID partial unique index
            mongoTemplate.indexOps(User.class)
                    .ensureIndex(
                            new Index()
                                    .on("googleId", Sort.Direction.ASC)
                                    .named("googleId_1")
                                    .unique()
                                    .partial(
                                            PartialIndexFilter.of(
                                                    new Document(
                                                            "googleId",
                                                            new Document("$type", "string")
                                                    )
                                            )
                                    )
                    );

            System.out.println("Google ID index verified successfully.");

            // GitHub ID partial unique index
            mongoTemplate.indexOps(User.class)
                    .ensureIndex(
                            new Index()
                                    .on("githubId", Sort.Direction.ASC)
                                    .named("githubId_1")
                                    .unique()
                                    .partial(
                                            PartialIndexFilter.of(
                                                    new Document(
                                                            "githubId",
                                                            new Document("$type", "string")
                                                    )
                                            )
                                    )
                    );

            System.out.println("GitHub ID index verified successfully.");
        };
    }
}