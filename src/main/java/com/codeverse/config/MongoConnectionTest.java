
package com.codeverse.config;

import org.bson.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.data.mongodb.core.MongoTemplate;

@Component
public class MongoConnectionTest implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    public MongoConnectionTest(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            mongoTemplate.getDb().runCommand(
                    new Document("ping", 1)
            );

            System.out.println("================================");
            System.out.println("MONGODB CONNECTION SUCCESSFUL!");
            System.out.println("CODEVERSE DATABASE IS CONNECTED!");
            System.out.println("================================");

        } catch (Exception e) {
            System.err.println("MONGODB CONNECTION FAILED!");
            e.printStackTrace();
        }
    }
}
