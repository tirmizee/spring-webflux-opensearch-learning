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
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

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
            var client = WebTestClient.bindToServer().baseUrl("http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port"))
                    .responseTimeout(Duration.ofSeconds(15)).build();
            String previousInfo = null;
            String previousHealth = null;
            for (String approach : new String[]{"spring-data", "java-client"}) {
                String prefix = "/api/" + approach + "/cluster";
                for (String endpoint : new String[]{"info", "health"}) {
                    boolean success = scenario.equals("monitor") || (scenario.equals("info") && endpoint.equals("info"));
                    var response = client.get().uri(prefix + "/" + endpoint).exchange()
                            .expectStatus().isEqualTo(success ? 200 : 502).expectBody();
                    if (success) {
                        response.jsonPath("$.clusterName").isEqualTo("learning-ch02");
                        if (endpoint.equals("info")) {
                            response.jsonPath("$.distribution").isEqualTo("opensearch")
                                    .jsonPath("$.version").isEqualTo("3.9.0");
                        } else {
                            response.jsonPath("$.numberOfNodes").isEqualTo(1).jsonPath("$.timedOut").isEqualTo(false);
                        }
                    } else {
                        String code = scenario.equals("info") ? "OPENSEARCH_AUTHORIZATION_FAILED"
                                : scenario.equals("invalid") ? "OPENSEARCH_AUTHENTICATION_FAILED" : "OPENSEARCH_TLS_FAILURE";
                        response.jsonPath("$.code").isEqualTo(code)
                                .jsonPath("$.instance").isEqualTo(prefix + "/" + endpoint);
                    }
                    var tree = JsonMapper.builder().build().readTree(response.returnResult().getResponseBody());
                    if (tree.isObject()) {
                        ((tools.jackson.databind.node.ObjectNode) tree).remove("instance");
                    }
                    var body = tree.toString();
                    if (endpoint.equals("info")) {
                        if (previousInfo != null) {
                            assertThat(body).isEqualTo(previousInfo);
                        }
                        previousInfo = body;
                    } else {
                        if (previousHealth != null) {
                            assertThat(body).isEqualTo(previousHealth);
                        }
                        previousHealth = body;
                    }
                }
            }
            var transport = context.getBean(OpenSearchTransport.class);
            assertThat(context.getBean(OpenSearchAsyncClient.class)._transport()).isSameAs(transport);
            assertThat(context.getBean(ReactiveOpenSearchClient.class)._transport()).isSameAs(transport);
        }
    }

    @Test
    void selectedClientsAreLazyAndTransportClosesWithContext() throws Exception {
        var properties = environment.properties("monitor", false);
        OpenSearchAsyncClient official;
        try (var context = startApplication(properties)) {
            official = context.getBean(OpenSearchAsyncClient.class);
            var springData = context.getBean(ReactiveOpenSearchClient.class);
            var spyOfficial = org.mockito.Mockito.spy(official);
            var spySpring = org.mockito.Mockito.spy(springData);
            var javaService = new JavaClientClusterConnectionService(spyOfficial, properties);
            var springService = new SpringDataClusterConnectionService(spySpring, properties);
            var javaResult = javaService.info();
            var springResult = springService.info();
            org.mockito.Mockito.verifyNoInteractions(spyOfficial, spySpring);
            StepVerifier.create(javaResult).assertNext(info -> assertThat(info.clusterName()).isEqualTo("learning-ch02")).verifyComplete();
            org.mockito.Mockito.verify(spyOfficial).info();
            org.mockito.Mockito.verifyNoInteractions(spySpring);
            StepVerifier.create(springResult).assertNext(info -> assertThat(info.clusterName()).isEqualTo("learning-ch02")).verifyComplete();
            org.mockito.Mockito.verify(spySpring).info();
        }
        var service = new JavaClientClusterConnectionService(official, properties);
        StepVerifier.create(service.info()).expectError().verify();
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
