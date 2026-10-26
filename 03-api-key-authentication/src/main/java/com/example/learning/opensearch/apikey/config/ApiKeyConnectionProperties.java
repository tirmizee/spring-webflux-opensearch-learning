package com.example.learning.opensearch.apikey.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.time.Duration;

@Validated
@ConfigurationProperties("opensearch")
public record ApiKeyConnectionProperties(@NotNull Path apiKeyFile, @NotNull Duration requestTimeout) {

    @AssertTrue(message = "Request timeout must be between 1ms and 60s")
    public boolean isRequestTimeoutValid() {
        return requestTimeout != null
                && requestTimeout.compareTo(Duration.ofMillis(1)) >= 0
                && requestTimeout.compareTo(Duration.ofSeconds(60)) <= 0;
    }
}
