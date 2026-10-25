package com.example.learning.opensearch.basicconnection.integration;

import com.example.learning.opensearch.basicconnection.config.OpenSearchProperties;
import com.example.learning.opensearch.basicconnection.dto.ClusterHealthResponse;
import com.example.learning.opensearch.basicconnection.dto.ClusterInfoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.util.ClassUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class OpenSearchConnectionIT {

    @Container
    static final GenericContainer<?> OPENSEARCH = new GenericContainer<>(DockerImageName.parse(System.getProperty("opensearch.image")))
            .withExposedPorts(9200)
            .withEnv("discovery.type", "single-node")
            .withEnv("cluster.name", "learning-ch01-it")
            .withEnv("node.name", "learning-ch01-it-node")
            .withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
            .withEnv("DISABLE_SECURITY_PLUGIN", "true")
            .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
            .waitingFor(Wait.forHttp("/_cluster/health?wait_for_status=yellow&timeout=3s")
                    .forStatusCode(200).withStartupTimeout(Duration.ofMinutes(3)));

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("learning.opensearch.uri", () -> "http://" + OPENSEARCH.getHost() + ":" + OPENSEARCH.getMappedPort(9200));
    }

    private WebTestClient http;
    @Autowired
    private ConfigurableApplicationContext context;
    @Autowired
    private OpenSearchProperties properties;

    @MockitoSpyBean
    private OpenSearchAsyncClient javaClient;
    @MockitoSpyBean
    private ReactiveOpenSearchClient springDataClient;

    @BeforeEach
    void connectToRunningApplication() {
        var port = context.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
        http = WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/java-client/cluster", "/api/spring-data/cluster", "/api/cluster"})
    void infoReflectsActualContainerThroughTheSelectedClient(String prefix) throws Exception {
        http.get().uri(prefix + "/info").exchange().expectStatus().isOk()
                .expectBody(ClusterInfoResponse.class).value(info -> {
                    assertThat(info.clusterName()).isEqualTo("learning-ch01-it");
                    assertThat(info.nodeName()).isEqualTo("learning-ch01-it-node");
                    assertThat(info.distribution()).isEqualTo("opensearch");
                    assertThat(info.version()).isEqualTo("3.9.0");
                    assertThat(info.clusterUuid()).isNotBlank().isNotEqualTo("_na_");
                });
        if (prefix.contains("spring-data")) {
            verify(springDataClient).info();
            verify(javaClient, never()).info();
        } else {
            verify(javaClient).info();
            verify(springDataClient, never()).info();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/java-client/cluster", "/api/spring-data/cluster", "/api/cluster"})
    void healthReflectsActualSingleNodeThroughTheSelectedClient(String prefix) {
        http.get().uri(prefix + "/health").exchange().expectStatus().isOk()
                .expectBody(ClusterHealthResponse.class).value(health -> {
                    assertThat(health.clusterName()).isEqualTo("learning-ch01-it");
                    assertThat(health.numberOfNodes()).isEqualTo(1);
                    assertThat(health.numberOfDataNodes()).isEqualTo(1);
                    assertThat(health.status()).isIn("green", "yellow");
                    assertThat(health.timedOut()).isFalse();
                });
        if (prefix.contains("spring-data")) {
            verify(springDataClient).cluster();
            verify(javaClient, never()).cluster();
        } else {
            verify(javaClient).cluster();
            verify(springDataClient, never()).cluster();
        }
    }

    @Test
    void bothApproachesAndLegacyAliasReturnEqualContracts() {
        var directInfo = info("/api/java-client/cluster");
        assertThat(info("/api/spring-data/cluster")).isEqualTo(directInfo);
        assertThat(info("/api/cluster")).isEqualTo(directInfo);
        var directHealth = health("/api/java-client/cluster");
        assertThat(health("/api/spring-data/cluster")).isEqualTo(directHealth);
        assertThat(health("/api/cluster")).isEqualTo(directHealth);
    }

    @Test
    void transportHasOneSpringManagedOwnerAndDynamicEndpoint() {
        assertThat(context.getBeansOfType(OpenSearchTransport.class)).hasSize(1);
        assertThat(properties.uri().getPort()).isEqualTo(OPENSEARCH.getMappedPort(9200));
        var transport = context.getBean(OpenSearchTransport.class);
        assertThat(javaClient._transport()).isSameAs(transport);
        assertThat(springDataClient._transport()).isSameAs(transport);
        assertThat(ClassUtils.isPresent("co.elastic.clients.elasticsearch.ElasticsearchClient", context.getClassLoader())).isFalse();
        assertThat(ClassUtils.isPresent("org.opensearch.client.RestHighLevelClient", context.getClassLoader())).isFalse();
    }

    private ClusterInfoResponse info(String prefix) {
        return http.get().uri(prefix + "/info").exchange().expectStatus().isOk()
                .expectBody(ClusterInfoResponse.class).returnResult().getResponseBody();
    }

    private ClusterHealthResponse health(String prefix) {
        return http.get().uri(prefix + "/health").exchange().expectStatus().isOk()
                .expectBody(ClusterHealthResponse.class).returnResult().getResponseBody();
    }
}
