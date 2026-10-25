package com.example.learning.opensearch.securitytls.integration;

import com.example.learning.opensearch.securitytls.SecurityTlsApplication;
import com.example.learning.opensearch.securitytls.config.OpenSearchProperties;
import com.example.learning.opensearch.securitytls.service.JavaClientClusterConnectionService;
import com.example.learning.opensearch.securitytls.service.SpringDataClusterConnectionService;
import com.example.learning.opensearch.securitytls.support.SecureOpenSearchTestEnvironment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport;
import org.opensearch.data.client.osc.ReactiveOpenSearchTemplate;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import tools.jackson.databind.node.ObjectNode;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SecureOpenSearchConnectionIT {

    private static SecureOpenSearchTestEnvironment environment;

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
    void nativeSecurityStatusCodesAndDeniedIndexCreation() throws Exception {
        assertThat(environment.raw("monitor", "GET", "/").statusCode()).isEqualTo(200);
        assertThat(environment.raw("invalid", "GET", "/").statusCode()).isEqualTo(401);
        assertThat(environment.raw("info", "GET", "/").statusCode()).isEqualTo(200);
        assertThat(environment.raw("info", "GET", "/_cluster/health").statusCode()).isEqualTo(403);
        assertThat(environment.raw("monitor", "PUT", "/ch02-forbidden-index").statusCode()).isEqualTo(403);
    }

    @ParameterizedTest
    @ValueSource(strings = {"monitor", "info", "invalid", "wrong-ca"})
    void realHttpContractsAndParityForBothPaths(String scenario) throws Exception {
        var properties = environment.properties(scenario.equals("wrong-ca") ? "monitor" : scenario, scenario.equals("wrong-ca"));
        try (var context = startApplication(properties)) {
            var client = httpClient(context);
            var previous = new HashMap<String, String>();
            for (String approach : new String[]{"spring-data", "java-client"}) {
                for (String endpoint : new String[]{"info", "health"}) {
                    var path = "/api/" + approach + "/cluster/" + endpoint;
                    var body = assertResponse(client, path, endpoint, expectedOutcome(scenario, endpoint));
                    assertParity(previous, endpoint, body);
                }
            }
            assertWiring(context);
        }
    }

    private static WebTestClient httpClient(ConfigurableApplicationContext context) {
        return WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port"))
                .responseTimeout(Duration.ofSeconds(15)).build();
    }

    private static ExpectedOutcome expectedOutcome(String scenario, String endpoint) {
        return switch (scenario) {
            case "monitor" -> new ExpectedOutcome(200, null);
            case "info" -> endpoint.equals("info") ? new ExpectedOutcome(200, null)
                    : new ExpectedOutcome(502, "OPENSEARCH_AUTHORIZATION_FAILED");
            case "invalid" -> new ExpectedOutcome(502, "OPENSEARCH_AUTHENTICATION_FAILED");
            case "wrong-ca" -> new ExpectedOutcome(502, "OPENSEARCH_TLS_FAILURE");
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
        };
    }

    private static String assertResponse(WebTestClient client, String path, String endpoint,
            ExpectedOutcome expected) throws Exception {
        var response = client.get().uri(path).exchange()
                .expectStatus().isEqualTo(expected.status()).expectBody();
        if (expected.status() == 200) {
            assertSuccess(response, endpoint);
        } else {
            response.jsonPath("$.code").isEqualTo(expected.code())
                    .jsonPath("$.instance").isEqualTo(path);
        }
        var tree = JsonMapper.builder().build().readTree(response.returnResult().getResponseBody());
        if (tree instanceof ObjectNode object) {
            object.remove("instance");
        }
        return tree.toString();
    }

    private static void assertSuccess(WebTestClient.BodyContentSpec response, String endpoint) {
        response.jsonPath("$.clusterName").isEqualTo("learning-ch02");
        if (endpoint.equals("info")) {
            response.jsonPath("$.distribution").isEqualTo("opensearch")
                    .jsonPath("$.version").isEqualTo("3.9.0");
        } else {
            response.jsonPath("$.numberOfNodes").isEqualTo(1)
                    .jsonPath("$.timedOut").isEqualTo(false);
        }
    }

    private static void assertParity(Map<String, String> previous, String endpoint, String body) {
        var first = previous.putIfAbsent(endpoint, body);
        if (first != null) {
            assertThat(body).isEqualTo(first);
        }
    }

    private static void assertWiring(ConfigurableApplicationContext context) {
        var official = context.getBean("javaClientTransport", OpenSearchTransport.class);
        var springData = context.getBean("springDataTransport", OpenSearchTransport.class);
        assertThat(context.getBeansOfType(OpenSearchTransport.class)).hasSize(2);
        assertThat(official).isInstanceOf(ApacheHttpClient5Transport.class);
        assertThat(springData).isInstanceOf(ApacheHttpClient5Transport.class).isNotSameAs(official);
        assertThat(context.getBean(OpenSearchAsyncClient.class)._transport()).isSameAs(official);
        assertThat(context.getBean(ReactiveOpenSearchClient.class)._transport()).isSameAs(springData);
        assertThat(context.getBean(ReactiveElasticsearchOperations.class)).isInstanceOf(ReactiveOpenSearchTemplate.class);
        assertThat(context.getBean(ElasticsearchConverter.class)).isNotNull();
    }

    private record ExpectedOutcome(int status, String code) {
    }

    @Test
    void selectedClientsAreLazyAndTransportClosesWithContext() throws Exception {
        var properties = environment.properties("monitor", false);
        OpenSearchAsyncClient official;
        ReactiveOpenSearchClient springData;
        try (var context = startApplication(properties)) {
            official = context.getBean(OpenSearchAsyncClient.class);
            springData = context.getBean(ReactiveOpenSearchClient.class);
            var spyOfficial = spy(official);
            var spySpring = spy(springData);
            var javaService = new JavaClientClusterConnectionService(spyOfficial, properties);
            var springService = new SpringDataClusterConnectionService(spySpring, properties);
            var javaResult = javaService.info();
            var springResult = springService.info();
            verifyNoInteractions(spyOfficial, spySpring);
            StepVerifier.create(javaResult).assertNext(info -> assertThat(info.clusterName()).isEqualTo("learning-ch02")).verifyComplete();
            verify(spyOfficial).info();
            verifyNoInteractions(spySpring);
            StepVerifier.create(springResult).assertNext(info -> assertThat(info.clusterName()).isEqualTo("learning-ch02")).verifyComplete();
            verify(spySpring).info();
        }
        var service = new JavaClientClusterConnectionService(official, properties);
        StepVerifier.create(service.info()).expectError().verify();
        var springService = new SpringDataClusterConnectionService(springData, properties);
        StepVerifier.create(springService.info()).expectError().verify();
    }

    private static ConfigurableApplicationContext startApplication(OpenSearchProperties properties) {
        return new SpringApplicationBuilder(SecurityTlsApplication.class).run(
                "--server.port=0", "--learning.opensearch.uri=" + properties.uri(),
                "--learning.opensearch.ca-certificate=" + properties.caCertificate(),
                "--learning.opensearch.username=" + properties.username(),
                "--learning.opensearch.password-file=" + properties.passwordFile(),
                "--logging.level.root=WARN");
    }
}
