package com.example.learning.opensearch.basicconnection.config;

import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSearchPropertiesTest {

    @ParameterizedTest
    @ValueSource(strings = {"https://localhost:9200", "http://user:pass@localhost:9200", "http://localhost:9200/path", "http://localhost:0", "http://localhost:9200?q=x"})
    void rejectsUnsupportedEndpoint(String uri) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var properties = new OpenSearchProperties(
                    URI.create(uri), Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(5)
            );
            assertThat(factory.getValidator().validate(properties)).isNotEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 60001})
    void rejectsUnboundedOrZeroTimeout(long milliseconds) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var properties = new OpenSearchProperties(
                    URI.create("http://localhost:9200"), Duration.ofMillis(milliseconds), Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(5)
            );
            assertThat(factory.getValidator().validate(properties)).isNotEmpty();
        }
    }
}
