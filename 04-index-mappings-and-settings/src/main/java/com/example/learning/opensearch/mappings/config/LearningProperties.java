package com.example.learning.opensearch.mappings.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("learning")
public record LearningProperties(@NotNull @Pattern(regexp = "ch04-[a-z0-9][a-z0-9_-]{0,50}") String indexName, @NotNull Duration requestTimeout) {

    @AssertTrue(message = "Request timeout must be between 1s and 60s")
    public boolean isDeadlineValid() {
        return requestTimeout != null && requestTimeout.compareTo(Duration.ofSeconds(1)) >= 0
                && requestTimeout.compareTo(Duration.ofSeconds(60)) <= 0;
    }
}
