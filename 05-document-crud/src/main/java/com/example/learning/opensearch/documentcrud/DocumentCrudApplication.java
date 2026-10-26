package com.example.learning.opensearch.documentcrud;

import com.example.learning.opensearch.documentcrud.config.LearningProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LearningProperties.class)
public class DocumentCrudApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentCrudApplication.class, args);
    }
}
