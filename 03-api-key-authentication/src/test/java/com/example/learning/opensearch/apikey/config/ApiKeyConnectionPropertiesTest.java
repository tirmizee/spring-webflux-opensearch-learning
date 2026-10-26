package com.example.learning.opensearch.apikey.config;

import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyConnectionPropertiesTest {

    @ParameterizedTest
    @CsvSource({"0,false", "-1,false", "1,true", "5000,true", "60000,true", "60001,false"})
    void validatesRequestDeadline(long milliseconds, boolean valid) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var settings = new ApiKeyConnectionProperties(Path.of("test-only.token"), Duration.ofMillis(milliseconds));
            assertThat(factory.getValidator().validate(settings).isEmpty()).isEqualTo(valid);
            assertThat(factory.getValidator().validate(new ApiKeyConnectionProperties(null, null))).isNotEmpty();
        }
    }
}
