package com.example.learning.opensearch.apikey.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenSearchApiKeyFileTest {

    @TempDir
    private Path temporary;

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "first\nsecond", "first\n\n", "first\rsecond", " token", "token ", "ApiKey token"})
    void rejectsBlankWhitespaceAndMultipleLines(String content) throws Exception {
        var file = temporary.resolve("token");
        Files.writeString(file, content);
        assertThatThrownBy(() -> OpenSearchApiKeyFile.read(file))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("API key file must contain one nonblank opaque token without whitespace");
    }

    @Test
    void acceptsOpaqueTokenAndOneFinalNewlineWithoutEncodingIt() throws Exception {
        var file = temporary.resolve("token");
        Files.writeString(file, "os_test-only_123-abc\r\n");
        assertThat(OpenSearchApiKeyFile.read(file)).isEqualTo("os_test-only_123-abc");
        Files.writeString(file, "x".repeat(4097));
        assertThatThrownBy(() -> OpenSearchApiKeyFile.read(file)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OpenSearchApiKeyFile.read(temporary.resolve("missing"))).isInstanceOf(java.io.IOException.class);
    }
}
