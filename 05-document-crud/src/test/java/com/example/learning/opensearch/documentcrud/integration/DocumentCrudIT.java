package com.example.learning.opensearch.documentcrud.integration;

import com.example.learning.opensearch.documentcrud.DocumentCrudApplication;
import com.example.learning.opensearch.documentcrud.domain.ProductDocument;
import com.example.learning.opensearch.documentcrud.repository.ProductRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.opensearch.data.client.osc.ReactiveOpenSearchTemplate;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentCrudIT {

    private static final String INDEX = "ch05-it-" + UUID.randomUUID().toString().replace("-", "");
    private static final String API = "/api/spring-data/products";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static GenericContainer<?> container;
    private static ConfigurableApplicationContext context;
    private static WebTestClient http;
    private static HttpClient nativeHttp;
    private static String nativeUrl;
    private static Map<String, Object> sample;
    private static Map<String, Object> replacement;

    @BeforeAll
    static void start() throws Exception {
        container = new GenericContainer<>(DockerImageName.parse(System.getProperty("opensearch.image")))
                .withExposedPorts(9200).withEnv("discovery.type", "single-node")
                .withEnv("action.auto_create_index", "+.*,-*")
                .withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true").withEnv("DISABLE_SECURITY_PLUGIN", "true")
                .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
                .waitingFor(Wait.forHttp("/_cluster/health?wait_for_status=yellow&timeout=3s")
                        .forStatusCode(200).withStartupTimeout(Duration.ofMinutes(3)));
        try {
            container.start();
            nativeUrl = "http://" + container.getHost() + ":" + container.getMappedPort(9200);
            nativeHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            sample = JSON.readValue(Files.readString(Path.of("fixtures/product.json")), Map.class);
            replacement = JSON.readValue(Files.readString(Path.of("fixtures/product-replacement.json")), Map.class);
            context = application(nativeUrl);
            http = http(context);
        } catch (Exception error) {
            stop();
            throw error;
        }
    }

    @AfterAll
    static void stop() {
        if (context != null) {
            context.close();
        }
        if (nativeHttp != null) {
            nativeHttp.close();
        }
        if (container != null) {
            container.close();
        }
    }

    @BeforeEach
    @AfterEach
    void cleanupOnlyOwnedIndex() throws Exception {
        assertThat(nativeRequest("DELETE", "/" + INDEX, null).statusCode()).isIn(200, 404);
    }

    @Test
    void starterWiringMappingAndRepositoryConverterRoundTrip() throws Exception {
        assertThat(nativeRequest("HEAD", "/" + INDEX, null).statusCode()).isEqualTo(404);
        assertThat(context.getBean(ProductRepository.class)).isNotNull();
        var transport = context.getBean(OpenSearchTransport.class);
        assertThat(transport.getClass().getName()).isEqualTo("org.opensearch.client.transport.rest_client.RestClientTransport");
        assertThat(context.getBean(ReactiveOpenSearchClient.class)._transport()).isSameAs(transport);
        var operations = context.getBean(ReactiveElasticsearchOperations.class);
        assertThat(operations).isInstanceOf(ReactiveOpenSearchTemplate.class);
        assertThat(((ReactiveOpenSearchTemplate) operations).getRefreshPolicy()).isEqualTo(RefreshPolicy.NONE);
        for (var bean : new String[]{"restClientTransport", "reactiveElasticsearchTemplate", "elasticsearchConverter"}) {
            assertThat(context.getBeanFactory().getBeanDefinition(bean).getFactoryBeanName())
                    .containsAnyOf("OpenSearchTransportConfiguration", "OpenSearchDataConfiguration");
        }
        createIndex(false);
        var persistedMapping = body(nativeRequest("GET", "/" + INDEX + "/_mapping", null)).path(INDEX).path("mappings");
        var expectedMapping = JSON.readTree(Files.readString(Path.of("src/main/resources/opensearch/product-index.json")))
                .path("mappings");
        StepVerifier.create(operations.indexOps(ProductDocument.class).createMapping())
                .assertNext(mapping -> {
                    var generated = JSON.valueToTree(mapping);
                    assertThat(generated.path("dynamic").asString()).isEqualTo("strict");
                    assertThat(generated.path("properties")).isEqualTo(expectedMapping.path("properties"));
                }).verifyComplete();
        assertThat(persistedMapping.path("properties")).isEqualTo(expectedMapping.path("properties"));
        http.put().uri(API + "/p-001").bodyValue(sample).exchange().expectStatus().isOk();
        http.get().uri(API + "/p-001").exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.id").isEqualTo("p-001").jsonPath("$.createdAt").isEqualTo("2026-01-01T00:00:00Z")
                .jsonPath("$.priceSatang").isEqualTo(129900).jsonPath("$.stock").isEqualTo(10);
        var source = body(nativeRequest("GET", "/" + INDEX + "/_doc/p-001", null)).path("_source");
        assertThat(source.has("id")).isFalse();
        assertThat(source.path("_class").asString()).isEqualTo(ProductDocument.class.getName());
        assertThat(source.path("stock").isNumber()).isTrue();
        var converted = context.getBean(ElasticsearchConverter.class).read(ProductDocument.class,
                org.springframework.data.elasticsearch.core.document.Document.parse(source.toString()));
        assertThat(converted.createdAt().toString()).isEqualTo("2026-01-01T00:00:00Z");
    }

    @Test
    void atomicCreateConflictsWithoutOverwritingAndSetsLocation() throws Exception {
        createIndex(false);
        http.post().uri(API + "/p-001").bodyValue(sample).exchange().expectStatus().isCreated()
                .expectHeader().valueEquals("Location", API + "/p-001");
        http.post().uri(API + "/p-001").bodyValue(replacement).exchange().expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("PRODUCT_ALREADY_EXISTS");
        http.get().uri(API + "/p-001").exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.name").isEqualTo("Mechanical Keyboard").jsonPath("$.stock").isEqualTo(10);
        var source = body(nativeRequest("GET", "/" + INDEX + "/_doc/p-001", null)).path("_source");
        assertThat(source.path("_class").asString()).isEqualTo(ProductDocument.class.getName());
        assertThat(source.has("id")).isFalse();
    }

    @Test
    void putCreatesAndFullyReplacesDocumentIncludingAllBusinessFields() throws Exception {
        createIndex(false);
        http.put().uri(API + "/p-001").bodyValue(sample).exchange().expectStatus().isOk();
        // Add an optional mapped field to prove replacement removes data omitted by the new document.
        assertThat(nativeRequest("PUT", "/" + INDEX + "/_mapping",
                Map.of("properties", Map.of("legacyNote", Map.of("type", "keyword")))).statusCode()).isEqualTo(200);
        assertThat(nativeRequest("POST", "/" + INDEX + "/_update/p-001",
                Map.of("doc", Map.of("legacyNote", "old"))).statusCode()).isEqualTo(200);
        http.put().uri(API + "/p-001").bodyValue(replacement).exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.name").isEqualTo("Wireless Keyboard").jsonPath("$.stock").isEqualTo(5)
                .jsonPath("$.available").isEqualTo(false);
        var source = body(nativeRequest("GET", "/" + INDEX + "/_doc/p-001", null)).path("_source");
        assertThat(source.has("legacyNote")).isFalse();
        assertThat(source.path("category").asString()).isEqualTo("accessories");
        assertThat(source.path("priceSatang").asLong()).isEqualTo(99000);
        assertThat(java.time.Instant.parse(source.path("createdAt").asString()))
                .isEqualTo(java.time.Instant.parse("2026-01-02T00:00:00Z"));
    }

    @Test
    void deleteAndMissingDocumentHaveConsistentContracts() throws Exception {
        createIndex(false);
        failure("GET", API + "/missing", 404, "PRODUCT_NOT_FOUND");
        failure("DELETE", API + "/missing", 404, "PRODUCT_NOT_FOUND");
        http.post().uri(API + "/p-001").bodyValue(sample).exchange().expectStatus().isCreated();
        http.delete().uri(API + "/p-001").exchange().expectStatus().isNoContent().expectBody().isEmpty();
        failure("GET", API + "/p-001", 404, "PRODUCT_NOT_FOUND");
        failure("DELETE", API + "/p-001", 404, "PRODUCT_NOT_FOUND");
        assertThat(nativeRequest("GET", "/" + INDEX + "/_doc/p-001", null).statusCode()).isEqualTo(404);
    }

    @Test
    void missingIndexIsNotReadyAndNeverAutoCreates() throws Exception {
        for (var method : new String[]{"GET", "DELETE"}) {
            failure(method, API + "/p-001", 503, "INDEX_NOT_READY");
        }
        for (var method : new String[]{"POST", "PUT"}) {
            http.method(HttpMethod.valueOf(method)).uri(API + "/p-001").bodyValue(sample).exchange()
                    .expectStatus().isEqualTo(503).expectBody().jsonPath("$.code").isEqualTo("INDEX_NOT_READY");
        }
        assertThat(nativeRequest("PUT", "/" + INDEX + "/_doc/native", Map.of("stock", 1)).statusCode()).isEqualTo(404);
        assertThat(nativeRequest("HEAD", "/" + INDEX, null).statusCode()).isEqualTo(404);
    }

    @Test
    void invalidJsonIdsAndFieldsAreRejectedWithoutPersisting() throws Exception {
        createIndex(false);
        http.post().uri(API + "/p-001").bodyValue(Map.of("name", " ")).exchange().expectStatus().isBadRequest();
        var extra = new java.util.HashMap<>(sample);
        extra.put("unknownField", "unexpected");
        http.put().uri(API + "/p-001").bodyValue(extra).exchange().expectStatus().isBadRequest().expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST");
        for (var field : new String[]{"stock", "priceSatang"}) {
            var invalid = new java.util.HashMap<>(sample);
            invalid.put(field, -1);
            http.put().uri(API + "/p-001").bodyValue(invalid).exchange().expectStatus().isBadRequest();
        }
        failure("GET", API + "/bad.id", 400, "INVALID_PRODUCT_ID");
        assertThat(nativeRequest("GET", "/" + INDEX + "/_doc/p-001", null).statusCode()).isEqualTo(404);
    }

    @Test
    void decimalIntegerInputsAreRejectedWithoutTruncation() throws Exception {
        createIndex(false);
        for (var field : new String[]{"stock", "priceSatang"}) {
            var invalid = new java.util.HashMap<>(sample);
            invalid.put(field, 1.5);
            http.post().uri(API + "/p-001").bodyValue(invalid).exchange().expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.code").isEqualTo("INVALID_REQUEST");
        }
        assertThat(nativeRequest("GET", "/" + INDEX + "/_doc/p-001", null).statusCode()).isEqualTo(404);
    }

    @Test
    void createRepositoryReplaceAndDeleteDoNotForceRefresh() throws Exception {
        createIndex(true);
        http.post().uri(API + "/p-001").bodyValue(sample).exchange().expectStatus().isCreated();
        http.get().uri(API + "/p-001").exchange().expectStatus().isOk();
        assertHits(0, null);
        refresh();
        assertHits(1, "Mechanical Keyboard");
        http.put().uri(API + "/p-001").bodyValue(replacement).exchange().expectStatus().isOk();
        http.get().uri(API + "/p-001").exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.name").isEqualTo("Wireless Keyboard");
        assertHits(1, "Mechanical Keyboard");
        refresh();
        assertHits(1, "Wireless Keyboard");
        http.delete().uri(API + "/p-001").exchange().expectStatus().isNoContent();
        failure("GET", API + "/p-001", 404, "PRODUCT_NOT_FOUND");
        assertHits(1, "Wireless Keyboard");
        refresh();
        assertHits(0, null);
    }

    @Test
    void unavailableBackendReturnsSanitized503() {
        try (var unavailable = application("http://127.0.0.1:1")) {
            http(unavailable).get().uri(API + "/p-001").exchange().expectStatus().isEqualTo(503).expectBody()
                    .jsonPath("$.code").isEqualTo("OPENSEARCH_UNAVAILABLE")
                    .jsonPath("$.detail").isEqualTo("The chapter operation could not be completed.");
        }
    }

    @Test
    void nativeStarterShutdownClosesTransport() {
        ReactiveOpenSearchClient client;
        try (var owned = application(nativeUrl)) {
            client = owned.getBean(ReactiveOpenSearchClient.class);
            StepVerifier.create(client.info()).expectNextCount(1).verifyComplete();
        }
        StepVerifier.create(client.info()).expectError().verify(Duration.ofSeconds(10));
    }

    private static void createIndex(boolean disableRefresh) throws Exception {
        var data = JSON.readTree(Files.readString(Path.of("src/main/resources/opensearch/product-index.json")));
        if (disableRefresh) {
            ((tools.jackson.databind.node.ObjectNode) data.path("settings")).put("refresh_interval", "-1");
        }
        assertThat(nativeRequest("PUT", "/" + INDEX, data).statusCode()).isEqualTo(200);
    }

    private static void refresh() throws Exception {
        assertThat(body(nativeRequest("POST", "/" + INDEX + "/_refresh", null)).path("_shards").path("failed").asInt()).isZero();
    }

    private static void assertHits(int expected, String name) throws Exception {
        var hits = body(nativeRequest("POST", "/" + INDEX + "/_search",
                Map.of("query", Map.of("match_all", Map.of()), "size", 10))).path("hits");
        assertThat(hits.path("total").path("relation").asString()).isEqualTo("eq");
        assertThat(hits.path("total").path("value").asInt()).isEqualTo(expected);
        if (expected == 1) {
            assertThat(hits.path("hits").get(0).path("_id").asString()).isEqualTo("p-001");
            assertThat(hits.path("hits").get(0).path("_source").path("name").asString()).isEqualTo(name);
        }
    }

    private static ConfigurableApplicationContext application(String uri) {
        return new SpringApplicationBuilder(DocumentCrudApplication.class, IsolatedServerResources.class).run("--server.port=0",
                "--opensearch.uris=" + uri, "--learning.index-name=" + INDEX, "--logging.level.root=WARN");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class IsolatedServerResources {

        @Bean
        ReactorResourceFactory reactorResourceFactory() {
            // Closing the failure/shutdown context must not dispose the healthy test server's global event loops.
            var resources = new ReactorResourceFactory();
            resources.setUseGlobalResources(false);
            return resources;
        }
    }

    private static WebTestClient http(ConfigurableApplicationContext application) {
        return WebTestClient.bindToServer().baseUrl("http://127.0.0.1:" + application.getEnvironment().getProperty("local.server.port"))
                .responseTimeout(Duration.ofSeconds(15)).build();
    }

    private static void failure(String method, String path, int status, String code) {
        http.method(HttpMethod.valueOf(method)).uri(path).exchange().expectStatus().isEqualTo(status)
                .expectBody().jsonPath("$.code").isEqualTo(code);
    }

    private static HttpResponse<String> nativeRequest(String method, String path, Object data) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(nativeUrl + path)).timeout(Duration.ofSeconds(10));
        var publisher = data == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(data));
        return nativeHttp.send(builder.header("Content-Type", "application/json").method(method, publisher).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static JsonNode body(HttpResponse<String> response) {
        assertThat(response.statusCode()).isBetween(200, 299);
        return JSON.readTree(response.body());
    }
}
