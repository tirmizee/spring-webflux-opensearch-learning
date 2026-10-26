package com.example.learning.opensearch.mappings.service;

import com.example.learning.opensearch.mappings.config.LearningProperties;
import com.example.learning.opensearch.mappings.domain.ProductDocument;
import com.example.learning.opensearch.mappings.dto.CreateProductSampleRequest;
import com.example.learning.opensearch.mappings.dto.ProductResponse;
import com.example.learning.opensearch.mappings.exception.ChapterException;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ProductSampleService {

    private final ReactiveElasticsearchOperations operations;
    private final ProductIndexService indices;
    private final LearningProperties properties;
    private final IndexCoordinates index;

    public ProductSampleService(ReactiveElasticsearchOperations operations, ProductIndexService indices,
            LearningProperties properties) {
        this.operations = operations;
        this.indices = indices;
        this.properties = properties;
        this.index = IndexCoordinates.of(properties.indexName());
    }

    public Mono<ProductResponse > save(String id, CreateProductSampleRequest request) {
        return validateId(id).then(indices.requireIndex(true))
                .then(Mono.defer(() -> operations.save(new ProductDocument(id, request.name(), request.category(),
                        request.priceSatang(), request.stock(), request.available(), request.createdAt()), index)))
                .map(ProductResponse::from).timeout(properties.requestTimeout());
    }

    public Mono<ProductResponse> get(String id) {
        return validateId(id).then(indices.requireIndex(false))
                .then(Mono.defer(() -> operations.get(id, ProductDocument.class, index)))
                .switchIfEmpty(Mono.error(new ChapterException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND")))
                .map(ProductResponse::from).timeout(properties.requestTimeout());
    }

    private static Mono<Void> validateId(String id) {
        return Mono.defer(() -> id != null && id.matches("[a-zA-Z0-9_-]{1,64}") ? Mono.empty()
                : Mono.error(new ChapterException(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_ID")));
    }
}
