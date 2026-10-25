package com.example.learning.opensearch.securitytls.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenSearchConfigurationTest {

    @TempDir
    private Path temporary;

    @Test
    void preservesPasswordSpacesAndRejectsMultipleLines() throws Exception {
        var password = temporary.resolve("password");
        Files.writeString(password, " secret \r\n");
        org.assertj.core.api.Assertions.assertThat(OpenSearchPasswordFile.read(password)).isEqualTo(" secret ");
        Files.writeString(password, "first\nsecond\n");
        assertThatThrownBy(() -> OpenSearchPasswordFile.read(password)).isInstanceOf(IllegalArgumentException.class);
        Files.writeString(password, "first\n\n");
        assertThatThrownBy(() -> OpenSearchPasswordFile.read(password)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OpenSearchPasswordFile.read(temporary.resolve("missing"))).isInstanceOf(java.io.IOException.class);
    }

    @Test
    void failsStartupForEmptyPasswordWithoutOpeningTransport() throws Exception {
        var cert = temporary.resolve("ca.pem");
        Files.copy(Path.of(OpenSearchConfigurationTest.class.getResource("/certs/unit-ca.pem").toURI()), cert);
        var password = temporary.resolve("password");
        Files.writeString(password, " ");
        var duration = java.time.Duration.ofSeconds(1);
        var properties = new OpenSearchProperties(java.net.URI.create("https://localhost:9200"), cert, "user", password,
                duration, duration, duration, duration, 16, 16);
        assertThatThrownBy(() -> OpenSearchTransportFactory.create(properties, JsonMapper.builder().build()))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Password file must contain one nonblank line");
    }
}
