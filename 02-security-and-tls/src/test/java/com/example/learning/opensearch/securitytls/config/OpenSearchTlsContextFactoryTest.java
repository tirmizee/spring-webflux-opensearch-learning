package com.example.learning.opensearch.securitytls.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenSearchTlsContextFactoryTest {

    @TempDir
    private Path temporary;

    @Test
    void rejectsMissingEmptyAndMalformedCa() throws Exception {
        assertThatThrownBy(() -> OpenSearchTlsContextFactory.create(temporary.resolve("missing"))).isInstanceOf(IOException.class);
        for (String value : new String[]{"", "not a certificate"}) {
            var file = temporary.resolve("ca.pem");
            Files.writeString(file, value);
            assertThatThrownBy(() -> OpenSearchTlsContextFactory.create(file)).isInstanceOf(GeneralSecurityException.class);
        }
    }

    @Test
    void createsIsolatedTrustContextWithoutChangingJvmDefault() throws Exception {
        var original = SSLContext.getDefault();
        var cert = temporary.resolve("ca.pem");
        Files.copy(Path.of(OpenSearchTlsContextFactoryTest.class.getResource("/certs/unit-ca.pem").toURI()), cert);
        assertThat(OpenSearchTlsContextFactory.create(cert)).isNotSameAs(original);
        assertThat(SSLContext.getDefault()).isSameAs(original);
    }
}
