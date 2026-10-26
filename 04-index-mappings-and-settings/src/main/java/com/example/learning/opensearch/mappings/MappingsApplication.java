package com.example.learning.opensearch.mappings;

import com.example.learning.opensearch.mappings.config.LearningProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LearningProperties.class)
public class MappingsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MappingsApplication.class, args);
    }
}
