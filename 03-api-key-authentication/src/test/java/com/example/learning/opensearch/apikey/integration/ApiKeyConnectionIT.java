package com.example.learning.opensearch.apikey.integration;

import com.example.learning.opensearch.apikey.ApiKeyApplication;
import com.example.learning.opensearch.apikey.support.SecureOpenSearchTestEnvironment.ApplicationSettings;
import com.example.learning.opensearch.apikey.service.ClusterConnectionService;
import com.example.learning.opensearch.apikey.support.SecureOpenSearchTestEnvironment;
import com.example.learning.opensearch.apikey.support.TestTlsContexts;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.opensearch.data.client.osc.ReactiveOpenSearchTemplate;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.ClassUtils;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;

@Isolated
class ApiKeyConnectionIT {

    private static SecureOpenSearchTestEnvironment environment;
    private static final String PREFIX = "/api/spring-data/cluster";

    @BeforeAll
    static void start() throws Exception {
        environment = new SecureOpenSearchTestEnvironment(false);
    }

    @AfterAll
    static void stop() throws Exception {
        if (environment != null) {
            environment.close();
        }
    }

    @Test
    void nativeScopedPermissionsAndAdministrationBoundary() throws Exception {
        var monitor = environment.issue("monitor", true, 120);
        var info = environment.issue("info", false, 120);
        assertThat(environment.keyed(monitor, "GET", "/").statusCode()).isEqualTo(200);
        assertThat(environment.keyed(monitor, "GET", "/_cluster/health").statusCode()).isEqualTo(200);
        assertThat(environment.keyed(info, "GET", "/").statusCode()).isEqualTo(200);
        assertThat(environment.keyed(info, "GET", "/_cluster/health").statusCode()).isEqualTo(403);
        assertThat(environment.keyed(monitor, "PUT", "/ch03-forbidden-index").statusCode()).isEqualTo(403);
        assertThat(environment.keyed(monitor, "GET", "/_plugins/_security/api/apitokens").statusCode()).isEqualTo(403);
        var list = environment.admin("GET", "/_plugins/_security/api/apitokens", null);
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.body().contains(Files.readString(monitor.file()))).isFalse();
        assertThat(list.body().contains(Files.readString(info.file()))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"monitor", "info", "invalid", "wrong-ca", "unavailable"})
    void httpContracts(String scenario) throws Exception {
        var key = environment.issue("http", !scenario.equals("info"), 120);
        Path file = key.file();
        if (scenario.equals("invalid")) {
            file = environment.material().resolve("secrets/invalid.token");
            Files.writeString(file, "os_invalid-test-token");
        }
        var properties = environment.properties(file);
        if (scenario.equals("unavailable")) {
            properties = new ApplicationSettings(java.net.URI.create("https://localhost:1"), file);
        }
        try (var context = application(properties, environment.material().resolve(
                scenario.equals("wrong-ca") ? "public/unrelated-ca.pem" : "public/ca.pem"))) {
            assertWiring(context);
            var http = http(context);
            for (String endpoint : new String[]{"info", "health"}) {
                if (scenario.equals("monitor") || (scenario.equals("info") && endpoint.equals("info"))) {
                    assertSuccess(http, endpoint);
                } else {
                    var code = switch (scenario) {
                        case "info" -> "OPENSEARCH_AUTHORIZATION_FAILED";
                        case "invalid" -> "OPENSEARCH_AUTHENTICATION_FAILED";
                        case "wrong-ca" -> "OPENSEARCH_TLS_FAILURE";
                        default -> "OPENSEARCH_UNAVAILABLE";
                    };
                    assertFailure(http, endpoint, scenario.equals("unavailable") ? 503 : 502, code);
                }
            }
        }
    }

    @Test
    void rotationRevokesCachedOldKeyWhileNewKeyRemainsUsable() throws Exception {
        var old = environment.issue("rotation_a", true, 120);
        var next = environment.issue("rotation_b", true, 120);
        try (var oldContext = application(environment.properties(old.file()));
             var nextContext = application(environment.properties(next.file()))) {
            var oldHttp = http(oldContext);
            var nextHttp = http(nextContext);
            assertSuccess(oldHttp, "info");
            assertSuccess(nextHttp, "info");
            environment.revoke(old);
            assertThat(environment.keyed(old, "GET", "/").statusCode()).isEqualTo(401);
            assertFailure(oldHttp, "info", 502, "OPENSEARCH_AUTHENTICATION_FAILED");
            assertFailure(oldHttp, "health", 502, "OPENSEARCH_AUTHENTICATION_FAILED");
            assertSuccess(nextHttp, "info");
            assertSuccess(nextHttp, "health");
        }
    }

    @Test
    void expirationRejectsPreviouslyAcceptedKeyWithoutRestartingClient() throws Exception {
        var key = environment.issue("expiry", true, 10);
        assertThat(environment.keyed(key, "GET", "/").statusCode()).isEqualTo(200);
        try (var context = application(environment.properties(key.file()))) {
            var http = http(context);
            assertSuccess(http, "info");
            var deadline = System.nanoTime() + Duration.ofSeconds(25).toNanos();
            int status = 200;
            while (System.nanoTime() < deadline) {
                status = environment.keyed(key, "GET", "/").statusCode();
                if (status == 401) {
                    break;
                }
                assertThat(status).isEqualTo(200);
                Thread.sleep(200);
            }
            assertThat(status).as("bounded expiry readiness").isEqualTo(401);
            assertFailure(http, "info", 502, "OPENSEARCH_AUTHENTICATION_FAILED");
            assertFailure(http, "health", 502, "OPENSEARCH_AUTHENTICATION_FAILED");
        }
    }

    @Test
    void closedContextClosesTransport() throws Exception {
        var key = environment.issue("lifecycle", true, 120);
        ClusterConnectionService service;
        try (var context = application(environment.properties(key.file()))) {
            service = context.getBean(ClusterConnectionService.class);
            StepVerifier.create(service.info()).expectNextCount(1).verifyComplete();
        }
        StepVerifier.create(service.info()).expectError().verify(Duration.ofSeconds(15));
    }

    @Test
    void trustedCertificateWithWrongSanFailsBeforeAuthentication() throws Exception {
        try (var mismatch = new SecureOpenSearchTestEnvironment(true)) {
            var factory = CertificateFactory.getInstance("X.509");
            X509Certificate leaf;
            X509Certificate ca;
            try (var input = Files.newInputStream(mismatch.material().resolve("http/mismatch.pem"))) {
                leaf = (X509Certificate) factory.generateCertificate(input);
            }
            try (var input = Files.newInputStream(mismatch.material().resolve("public/ca.pem"))) {
                ca = (X509Certificate) factory.generateCertificate(input);
            }
            leaf.checkValidity();
            leaf.verify(ca.getPublicKey());
            assertThat(leaf.getSubjectAlternativeNames().toString()).contains("wrong.invalid").doesNotContain("localhost");
            var file = mismatch.material().resolve("secrets/hostname.token");
            Files.writeString(file, "os_test-only");
            try (var context = application(mismatch.properties(file), mismatch.material().resolve("public/ca.pem"))) {
                assertFailure(http(context), "info", 502, "OPENSEARCH_TLS_FAILURE");
                assertFailure(http(context), "health", 502, "OPENSEARCH_TLS_FAILURE");
            }
        }
    }

    private static void assertWiring(ConfigurableApplicationContext context) {
        assertThat(context.getBean(OpenSearchTransport.class).getClass().getName())
                .isEqualTo("org.opensearch.client.transport.rest_client.RestClientTransport");
        assertThat(context.getBeanFactory().getBeanDefinition("restClientTransport").getFactoryBeanName())
                .contains("OpenSearchTransportConfiguration");
        assertThat(context.getBeanFactory().getBeanDefinition("reactiveOpensearchClient").getFactoryBeanName())
                .contains("ReactiveOpenSearchClientAutoConfiguration");
        assertThat(context.getBean(ReactiveOpenSearchClient.class)._transport()).isSameAs(context.getBean(OpenSearchTransport.class));
        assertThat(context.getBean(ReactiveElasticsearchOperations.class)).isInstanceOf(ReactiveOpenSearchTemplate.class);
        assertThat(ClassUtils.isPresent("co.elastic.clients.transport.rest5_client.low_level.Rest5Client", context.getClassLoader())).isFalse();
        assertThat(context.containsBean("opensearchRestClient")).isTrue();
        assertThat(context.getBeanFactory().getBeanDefinition("reactiveElasticsearchTemplate").getFactoryBeanName())
                .contains("OpenSearchDataConfiguration");
    }

    private static ConfigurableApplicationContext application(ApplicationSettings properties) throws Exception {
        return application(properties, environment.material().resolve("public/ca.pem"));
    }

    private static ConfigurableApplicationContext application(ApplicationSettings properties, Path ca) throws Exception {
        // Test-only trust isolation. Production reads the JVM default context; tests run sequentially.
        var original = SSLContext.getDefault();
        try {
            SSLContext.setDefault(TestTlsContexts.create(ca));
            return new SpringApplicationBuilder(ApiKeyApplication.class).run("--server.port=0",
                    "--opensearch.uris=" + properties.uri(),
                    "--opensearch.api-key-file=" + properties.apiKeyFile(),
                    "--opensearch.socket-timeout=3s", "--opensearch.request-timeout=5s",
                    "--logging.level.root=WARN");
        } finally {
            SSLContext.setDefault(original);
        }
    }

    private static WebTestClient http(ConfigurableApplicationContext context) {
        return WebTestClient.bindToServer().baseUrl("http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port"))
                .responseTimeout(Duration.ofSeconds(15)).build();
    }

    private static void assertSuccess(WebTestClient http, String endpoint) {
        var body = http.get().uri(PREFIX + "/" + endpoint).exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.clusterName").isEqualTo("learning-ch03");
        if (endpoint.equals("info")) {
            body.jsonPath("$.distribution").isEqualTo("opensearch").jsonPath("$.version").isEqualTo("3.9.0");
        } else {
            body.jsonPath("$.numberOfNodes").isEqualTo(1).jsonPath("$.timedOut").isEqualTo(false);
        }
    }

    private static void assertFailure(WebTestClient http, String endpoint, int status, String code) {
        http.get().uri(PREFIX + "/" + endpoint).exchange().expectStatus().isEqualTo(status).expectBody()
                .jsonPath("$.code").isEqualTo(code).jsonPath("$.instance").isEqualTo(PREFIX + "/" + endpoint);
    }
}
