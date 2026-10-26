package com.example.learning.opensearch.mappings.service;

import com.example.learning.opensearch.mappings.config.LearningProperties;
import com.example.learning.opensearch.mappings.domain.ProductDocument;
import com.example.learning.opensearch.mappings.dto.IndexMappingResponse;
import com.example.learning.opensearch.mappings.dto.IndexOperationResponse;
import com.example.learning.opensearch.mappings.dto.IndexSettingsResponse;
import com.example.learning.opensearch.mappings.dto.UpdateIndexSettingsRequest;
import com.example.learning.opensearch.mappings.exception.ChapterException;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class ProductIndexService {

    private final ReactiveElasticsearchOperations operations;
    private final ReactiveOpenSearchClient client;
    private final LearningProperties properties;
    private final IndexCoordinates index;
    private final Map<String, Object> settings;

    public ProductIndexService(ReactiveElasticsearchOperations operations, ReactiveOpenSearchClient client,
            LearningProperties properties) throws IOException {
        this.operations = operations;
        this.client = client;
        this.properties = properties;
        this.index = IndexCoordinates.of(properties.indexName());
        // Fixed classpath resource is loaded only during startup, never on an event-loop request.
        this.settings = Document.parse(new ClassPathResource("opensearch/product-index-settings.json")
                .getContentAsString(StandardCharsets.UTF_8));
    }

    public Mono<IndexOperationResponse> create() {
        return Mono.defer(() -> operations.indexOps(index).createMapping(ProductDocument.class)
                .flatMap(mapping -> operations.indexOps(index).create(settings, mapping))
                .flatMap(acknowledged -> acknowledged
                        ? Mono.just(new IndexOperationResponse(properties.indexName(), true))
                        : Mono.error(new ChapterException(HttpStatus.GATEWAY_TIMEOUT, "INDEX_NOT_ACKNOWLEDGED"))))
                .timeout(properties.requestTimeout());
    }

    public Mono<IndexMappingResponse> mapping() {
        return requireIndex(false).then(Mono.defer(() -> operations.indexOps(index).getMapping()))
                .map(mapping -> new IndexMappingResponse(properties.indexName(), mapping))
                .timeout(properties.requestTimeout());
    }

    public Mono<IndexSettingsResponse> settings() {
        return requireIndex(false).then(Mono.defer(() -> operations.indexOps(index).getSettings()))
                .map(settings -> new IndexSettingsResponse(properties.indexName(),
                        Integer.parseInt(value(settings, "number_of_shards")),
                        Integer.parseInt(value(settings, "number_of_replicas")),
                        value(settings, "refresh_interval"), Boolean.parseBoolean(value(settings, "mapping.coerce"))))
                .timeout(properties.requestTimeout());
    }

    public Mono<IndexSettingsResponse> updateSettings(UpdateIndexSettingsRequest request) {
        return requireIndex(false).then(Mono.defer(() -> client.indices().putSettings(builder -> builder
                        .index(properties.indexName()).settings(settings -> settings
                                .numberOfReplicas(request.numberOfReplicas())
                                .refreshInterval(time -> time.time(request.refreshInterval()))))))
                .flatMap(response -> response.acknowledged() ? settings()
                        : Mono.error(new ChapterException(HttpStatus.GATEWAY_TIMEOUT, "INDEX_NOT_ACKNOWLEDGED")))
                .timeout(properties.requestTimeout());
    }

    public Mono<Void> delete() {
        return Mono.defer(() -> operations.indexOps(index).delete())
                .flatMap(deleted -> deleted ? Mono.<Void>empty()
                        : Mono.error(new ChapterException(HttpStatus.NOT_FOUND, "INDEX_NOT_FOUND")))
                .timeout(properties.requestTimeout());
    }

    public Mono<Void> requireIndex(boolean forWrite) {
        return Mono.defer(() -> operations.indexOps(index).exists())
                .flatMap(exists -> exists ? Mono.<Void>empty() : Mono.error(new ChapterException(
                        forWrite ? HttpStatus.CONFLICT : HttpStatus.NOT_FOUND,
                        forWrite ? "INDEX_NOT_READY" : "INDEX_NOT_FOUND")))
                .timeout(properties.requestTimeout());
    }

    private static String value(Settings settings, String key) {
        var flat = settings.flatten();
        var result = flat.get("index." + key);
        if (result == null) {
            result = flat.get(key);
        }
        if (result == null) {
            throw new IllegalStateException("Expected index setting missing");
        }
        return result.toString();
    }
}
