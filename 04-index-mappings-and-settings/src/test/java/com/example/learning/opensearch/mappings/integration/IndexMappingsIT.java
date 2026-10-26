package com.example.learning.opensearch.mappings.integration;

import com.example.learning.opensearch.mappings.MappingsApplication;
import com.example.learning.opensearch.mappings.domain.ProductDocument;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.opensearch.data.client.osc.ReactiveOpenSearchTemplate;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.http.HttpMethod;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IndexMappingsIT {

    private static final String INDEX = "ch04-it-" + UUID.randomUUID().toString().replace("-", "");
    private static final String INDEX_API = "/api/spring-data/indices/products";
    private static final String SAMPLE_API = "/api/spring-data/mapping-examples/products";
    private static final Map<String, Object> SAMPLE = Map.of("name", "Mechanical Keyboard", "category", "electronics",
            "priceSatang", 129900, "stock", 10, "available", true, "createdAt", "2026-01-01T00:00:00Z");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static GenericContainer<?> container;
    private static ConfigurableApplicationContext context;
    private static WebTestClient http;
    private static HttpClient nativeHttp;
    private static String nativeUrl;

    @BeforeAll
    static void start() {
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
            context = application(nativeUrl);
            http = http(context);
        } catch (RuntimeException error) {
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
    void explicitMappingSettingsAndConverterRoundTrip() throws Exception {
        assertFailure("GET", INDEX_API + "/mapping", 404, "INDEX_NOT_FOUND");
        create();
        assertWiring(context);
        var mapping = body(nativeRequest("GET", "/" + INDEX + "/_mapping", null)).path(INDEX).path("mappings");
        assertThat(mapping.path("dynamic").asString()).isEqualTo("strict");
        var fields = mapping.path("properties");
        assertThat(fields.path("name").path("type").asString()).isEqualTo("text");
        assertThat(fields.path("name").path("fields").path("raw").path("type").asString()).isEqualTo("keyword");
        assertThat(fields.path("category").path("type").asString()).isEqualTo("keyword");
        assertThat(fields.path("priceSatang").path("type").asString()).isEqualTo("long");
        assertThat(fields.path("stock").path("type").asString()).isEqualTo("integer");
        assertThat(fields.path("available").path("type").asString()).isEqualTo("boolean");
        assertThat(fields.path("createdAt").path("type").asString()).isEqualTo("date");
        assertThat(fields.path("_class").path("type").asString()).isEqualTo("keyword");
        http.get().uri(INDEX_API + "/mapping").exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.indexName").isEqualTo(INDEX).jsonPath("$.mapping.dynamic").isEqualTo("strict");
        assertSettings(0, "1s");
        save();
        http.get().uri(SAMPLE_API + "/p-001").exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.id").isEqualTo("p-001").jsonPath("$.priceSatang").isEqualTo(129900)
                .jsonPath("$.stock").isEqualTo(10).jsonPath("$.createdAt").isEqualTo("2026-01-01T00:00:00Z");
        var source = body(nativeRequest("GET", "/" + INDEX + "/_doc/p-001", null)).path("_source");
        assertThat(source.has("id")).isFalse();
        assertThat(source.path("_class").asString()).isEqualTo(ProductDocument.class.getName());
        assertThat(source.path("priceSatang").asLong()).isEqualTo(129900L);
        var converter = context.getBean(ElasticsearchConverter.class);
        var entity = new ProductDocument("roundtrip", "Keyboard", "electronics", 129900L, 10, true,
                Instant.parse("2026-01-01T00:00:00Z"));
        var document = converter.mapObject(entity);
        var converted = converter.read(ProductDocument.class, document);
        assertThat(converted.name()).isEqualTo(entity.name());
        assertThat(converted.createdAt()).isEqualTo(entity.createdAt());
        assertThat(converted.priceSatang()).isEqualTo(entity.priceSatang());
        assertThat(nativeRequest("POST", "/" + INDEX + "/_refresh", null).statusCode()).isEqualTo(200);
        var found = body(nativeRequest("POST", "/" + INDEX + "/_search",
                Map.of("query", Map.of("term", Map.of("name.raw", "Mechanical Keyboard")))));
        assertThat(found.path("hits").path("total").path("value").asInt()).isEqualTo(1);
        assertThat(found.path("hits").path("hits").get(0).path("_id").asString()).isEqualTo("p-001");
    }

    @Test
    void nativeServerRejectsWrongTypesAndStrictUnknownFields() throws Exception {
        create();
        assertNativeRejected(Map.of("stock", "10"), "mapper_parsing_exception");
        assertNativeRejected(Map.of("stock", "abc"), "mapper_parsing_exception");
        assertNativeRejected(Map.of("unknownField", "unexpected"), "strict_dynamic_mapping_exception");
        assertThat(nativeRequest("GET", "/" + INDEX + "/_doc/bad", null).statusCode()).isEqualTo(404);
        var changeType = nativeRequest("PUT", "/" + INDEX + "/_mapping",
                Map.of("properties", Map.of("stock", Map.of("type", "keyword"))));
        assertThat(changeType.statusCode()).isEqualTo(400);
        assertThat(changeType.body()).contains("illegal_argument_exception");
        var finalSetting = nativeRequest("PUT", "/" + INDEX + "/_settings", Map.of("index.number_of_shards", 2));
        assertThat(finalSetting.statusCode()).isEqualTo(400);
        assertThat(finalSetting.body()).contains("illegal_argument_exception", "index.number_of_shards");
        assertThat(nativeRequest("POST", "/" + INDEX + "/_close", null).statusCode()).isEqualTo(200);
        var closedSetting = nativeRequest("PUT", "/" + INDEX + "/_settings", Map.of("index.number_of_shards", 2));
        assertThat(closedSetting.statusCode()).isEqualTo(400);
        assertThat(closedSetting.body()).contains("final", "index.number_of_shards");
    }

    @Test
    void dynamicSettingsChangeAndReadBackWhileStaticSettingsStayFixed() {
        create();
        http.patch().uri(INDEX_API + "/settings").bodyValue(Map.of("numberOfReplicas", 1, "refreshInterval", "2s"))
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.numberOfReplicas").isEqualTo(1)
                .jsonPath("$.refreshInterval").isEqualTo("2s");
        assertSettings(1, "2s");
        http.patch().uri(INDEX_API + "/settings").bodyValue(Map.of("numberOfReplicas", 0, "refreshInterval", "1s"))
                .exchange().expectStatus().isOk();
        assertSettings(0, "1s");
        http.patch().uri(INDEX_API + "/settings").bodyValue(Map.of("numberOfReplicas", -1, "refreshInterval", "0s"))
                .exchange().expectStatus().isBadRequest();
        assertSettings(0, "1s");
        http.patch().uri(INDEX_API + "/settings").bodyValue(Map.of("numberOfReplicas", 0,
                        "refreshInterval", "1s", "numberOfShards", 2))
                .exchange().expectStatus().isBadRequest().expectBody().jsonPath("$.code").isEqualTo("INVALID_REQUEST");
        assertSettings(0, "1s");
    }

    @Test
    void lifecycleHasNoAutoCreationAndRepresentativeFailures() throws Exception {
        assertThat(nativeRequest("HEAD", "/" + INDEX, null).statusCode()).isEqualTo(404);
        http.put().uri(SAMPLE_API + "/p-001").bodyValue(SAMPLE).exchange().expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("INDEX_NOT_READY");
        assertThat(nativeRequest("PUT", "/" + INDEX + "/_doc/native", Map.of("stock", 1)).statusCode()).isEqualTo(404);
        assertThat(nativeRequest("HEAD", "/" + INDEX, null).statusCode()).isEqualTo(404);
        assertFailure("GET", SAMPLE_API + "/missing", 404, "INDEX_NOT_FOUND");
        create();
        assertFailure("POST", INDEX_API, 409, "INDEX_ALREADY_EXISTS");
        assertFailure("GET", SAMPLE_API + "/missing", 404, "PRODUCT_NOT_FOUND");
        http.put().uri(SAMPLE_API + "/p-001").bodyValue(Map.of("name", " ")).exchange().expectStatus().isBadRequest();
        save();
        http.delete().uri(INDEX_API).exchange().expectStatus().isNoContent();
        assertFailure("DELETE", INDEX_API, 404, "INDEX_NOT_FOUND");
        assertFailure("GET", INDEX_API + "/settings", 404, "INDEX_NOT_FOUND");
        http.patch().uri(INDEX_API + "/settings").bodyValue(Map.of("numberOfReplicas", 0, "refreshInterval", "2s"))
                .exchange().expectStatus().isNotFound().expectBody().jsonPath("$.code").isEqualTo("INDEX_NOT_FOUND");
    }

    @Test
    void unavailableBackendReturnsSanitized503() {
        // Port 1 is deliberately unavailable; this is separate from the owned healthy cluster.
        try (var unavailable = application("http://127.0.0.1:1")) {
            http(unavailable).get().uri(INDEX_API + "/mapping").exchange().expectStatus().isEqualTo(503)
                    .expectBody().jsonPath("$.code").isEqualTo("OPENSEARCH_UNAVAILABLE")
                    .jsonPath("$.detail").isEqualTo("The chapter operation could not be completed.");
        }
    }

    @Test
    void closingApplicationClosesNativeTransport() {
        ReactiveOpenSearchClient client;
        try (var owned = application(nativeUrl)) {
            client = owned.getBean(ReactiveOpenSearchClient.class);
            StepVerifier.create(client.info()).expectNextCount(1).verifyComplete();
        }
        StepVerifier.create(client.info()).expectError().verify(Duration.ofSeconds(10));
    }

    private static ConfigurableApplicationContext application(String uri) {
        return new SpringApplicationBuilder(MappingsApplication.class).run("--server.port=0",
                "--opensearch.uris=" + uri, "--learning.index-name=" + INDEX, "--logging.level.root=WARN");
    }

    private static WebTestClient http(ConfigurableApplicationContext application) {
        return WebTestClient.bindToServer().baseUrl("http://127.0.0.1:" + application.getEnvironment().getProperty("local.server.port"))
                .responseTimeout(Duration.ofSeconds(15)).build();
    }

    private static void assertWiring(ConfigurableApplicationContext application) {
        var transport = application.getBean(OpenSearchTransport.class);
        assertThat(transport.getClass().getName()).isEqualTo("org.opensearch.client.transport.rest_client.RestClientTransport");
        assertThat(application.getBean(ReactiveOpenSearchClient.class)._transport()).isSameAs(transport);
        assertThat(application.getBean(ReactiveElasticsearchOperations.class)).isInstanceOf(ReactiveOpenSearchTemplate.class);
        assertThat(application.getBeanFactory().getBeanDefinition("reactiveElasticsearchTemplate").getFactoryBeanName())
                .contains("OpenSearchDataConfiguration");
    }

    private static void create() {
        http.post().uri(INDEX_API).exchange().expectStatus().isCreated().expectBody()
                .jsonPath("$.indexName").isEqualTo(INDEX).jsonPath("$.acknowledged").isEqualTo(true);
    }

    private static void save() {
        http.put().uri(SAMPLE_API + "/p-001").bodyValue(SAMPLE).exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.id").isEqualTo("p-001").jsonPath("$.name").isEqualTo("Mechanical Keyboard");
    }

    private static void assertSettings(int replicas, String refresh) {
        http.get().uri(INDEX_API + "/settings").exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.numberOfShards").isEqualTo(1).jsonPath("$.numberOfReplicas").isEqualTo(replicas)
                .jsonPath("$.refreshInterval").isEqualTo(refresh).jsonPath("$.coerce").isEqualTo(false);
    }

    private static void assertFailure(String method, String path, int status, String code) {
        http.method(HttpMethod.valueOf(method)).uri(path).exchange()
                .expectStatus().isEqualTo(status).expectBody().jsonPath("$.code").isEqualTo(code);
    }

    private static void assertNativeRejected(Map<String, Object> document, String errorType) throws Exception {
        var result = nativeRequest("PUT", "/" + INDEX + "/_doc/bad", document);
        assertThat(result.statusCode()).isEqualTo(400);
        assertThat(result.body()).contains(errorType);
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
