package com.example.learning.opensearch.apikey.config;

import com.example.learning.opensearch.apikey.support.TestTlsContexts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestTlsContextsTest {

    @TempDir
    private Path temporary;

    @Test
    void rejectsMissingEmptyAndMalformedCa() throws Exception {
        assertThatThrownBy(() -> TestTlsContexts.create(temporary.resolve("missing"))).isInstanceOf(IOException.class);
        for (String value : new String[]{"", "not a certificate"}) {
            var file = temporary.resolve("ca.pem");
            Files.writeString(file, value);
            assertThatThrownBy(() -> TestTlsContexts.create(file)).isInstanceOf(GeneralSecurityException.class);
        }
    }

    @Test
    void createsIsolatedTrustContextWithoutChangingJvmDefault() throws Exception {
        var original = SSLContext.getDefault();
        var cert = temporary.resolve("ca.pem");
        Files.copy(Path.of(TestTlsContextsTest.class.getResource("/certs/unit-ca.pem").toURI()), cert);
        assertThat(TestTlsContexts.create(cert)).isNotSameAs(original);
        assertThat(SSLContext.getDefault()).isSameAs(original);
    }
}
