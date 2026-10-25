package com.example.learning.opensearch.securitytls.config;

import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSearchPropertiesTest {

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:9200", "https://user:pass@localhost:9200", "https://localhost/path", "https://localhost:0", "https://localhost?q=1", "https://localhost#fragment"})
    void rejectsUnsafeEndpoint(String uri) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(properties(uri, "user", Duration.ofSeconds(1)))).isNotEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://localhost:9202", "https://127.0.0.1:9202", "https://localhost/"})
    void acceptsHttpsWithoutEmbeddedCredentials(String uri) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(properties(uri, "user", Duration.ofSeconds(1)))).isEmpty();
            assertThat(factory.getValidator().validate(properties(uri, " ", Duration.ofSeconds(1)))).isNotEmpty();
            assertThat(factory.getValidator().validate(properties(uri, "user", Duration.ZERO))).isNotEmpty();
        }
    }

    private OpenSearchProperties properties(String uri, String user, Duration timeout) {
        return new OpenSearchProperties(URI.create(uri), Path.of("ca.pem"), user, Path.of("password"),
                timeout, timeout, timeout, timeout);
    }
}
