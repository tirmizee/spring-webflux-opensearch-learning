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
        var process = new ProcessBuilder("openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes",
                "-days", "1", "-subj", "/CN=Unit Test CA", "-keyout", temporary.resolve("ca.key").toString(),
                "-out", cert.toString()).redirectErrorStream(true).start();
        process.getInputStream().readAllBytes();
        assertThat(process.waitFor()).isZero();
        assertThat(OpenSearchTlsContextFactory.create(cert)).isNotSameAs(original);
        assertThat(SSLContext.getDefault()).isSameAs(original);
    }
}
