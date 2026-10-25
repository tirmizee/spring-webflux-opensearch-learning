package com.example.learning.opensearch.securitytls.integration;

import com.example.learning.opensearch.securitytls.config.OpenSearchConfiguration;
import com.example.learning.opensearch.securitytls.config.SpringDataOpenSearchConfiguration;
import com.example.learning.opensearch.securitytls.service.JavaClientClusterConnectionService;
import com.example.learning.opensearch.securitytls.service.SpringDataClusterConnectionService;
import com.example.learning.opensearch.securitytls.exception.ApiExceptionHandler;
import com.example.learning.opensearch.securitytls.support.SecureOpenSearchTestEnvironment;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.security.cert.CertificateFactory;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSearchTlsFailureIT {

    @Test
    void trustedValidCertificateWithWrongSanFailsBothClientsBeforeHttp() throws Exception {
        try (var environment = new SecureOpenSearchTestEnvironment(true)) {
            var certificates = CertificateFactory.getInstance("X.509");
            java.security.cert.X509Certificate leaf;
            java.security.cert.X509Certificate ca;
            try (var input = Files.newInputStream(environment.material().resolve("http/mismatch.pem"))) {
                leaf = (java.security.cert.X509Certificate) certificates.generateCertificate(input);
            }
            try (var input = Files.newInputStream(environment.material().resolve("public/ca.pem"))) {
                ca = (java.security.cert.X509Certificate) certificates.generateCertificate(input);
            }
            leaf.checkValidity();
            leaf.verify(ca.getPublicKey());
            assertThat(leaf.getSubjectAlternativeNames().toString()).contains("wrong.invalid").doesNotContain("localhost", "127.0.0.1");
            var properties = environment.properties("monitor", false);
            new ApplicationContextRunner().withUserConfiguration(OpenSearchConfiguration.class, SpringDataOpenSearchConfiguration.class)
                    .withBean(JsonMapper.class, () -> JsonMapper.builder().build())
                    .withPropertyValues("learning.opensearch.uri=" + properties.uri(),
                            "learning.opensearch.ca-certificate=" + properties.caCertificate(),
                            "learning.opensearch.username=" + properties.username(),
                            "learning.opensearch.password-file=" + properties.passwordFile(),
                            "learning.opensearch.connect-timeout=2s", "learning.opensearch.pool-timeout=2s",
                            "learning.opensearch.response-timeout=5s", "learning.opensearch.request-timeout=10s")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        var official = new JavaClientClusterConnectionService(context.getBean(org.opensearch.client.opensearch.OpenSearchAsyncClient.class), properties);
                        var spring = new SpringDataClusterConnectionService(context.getBean(org.opensearch.data.client.osc.ReactiveOpenSearchClient.class), properties);
                        for (var result : java.util.List.of(official.info(), spring.info())) {
                            StepVerifier.create(result).expectErrorSatisfies(failure -> {
                                assertThat(SecureOpenSearchTestEnvironment.hasTlsCause(failure)).isTrue();
                                var problem = new ApiExceptionHandler().upstream((Exception) failure);
                                assertThat(problem.getStatus()).isEqualTo(502);
                                assertThat(problem.getProperties()).containsEntry("code", "OPENSEARCH_TLS_FAILURE");
                            }).verify();
                        }
                    });
        }
    }
}
