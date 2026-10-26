package com.example.learning.opensearch.documentcrud.service;

import com.example.learning.opensearch.documentcrud.config.LearningProperties;
import com.example.learning.opensearch.documentcrud.domain.ProductDocument;
import com.example.learning.opensearch.documentcrud.dto.ProductRequest;
import com.example.learning.opensearch.documentcrud.dto.ProductResponse;
import com.example.learning.opensearch.documentcrud.exception.ChapterException;
import com.example.learning.opensearch.documentcrud.repository.ProductRepository;
import org.opensearch.client.opensearch._types.OpType;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final ReactiveElasticsearchOperations operations;
    private final ReactiveOpenSearchClient client;
    private final LearningProperties properties;
    private final RequestLimit limit;
    private final IndexCoordinates index;

    public ProductService(ProductRepository repository, ReactiveElasticsearchOperations operations,
            ReactiveOpenSearchClient client, LearningProperties properties, RequestLimit limit) {
        this.repository = repository;
        this.operations = operations;
        this.client = client;
        this.properties = properties;
        this.limit = limit;
        this.index = IndexCoordinates.of(properties.indexName());
    }

    public Mono<ProductResponse> create(String id, ProductRequest request) {
        return execute(id, () -> {
            var entity = document(id, request);
            // Native reactive-client tier for atomic create-only; retain Spring Data conversion semantics.
            var source = operations.getElasticsearchConverter().mapObject(entity);
            var command = new IndexRequest.Builder<org.springframework.data.elasticsearch.core.document.Document>()
                    .index(properties.indexName()).id(id).opType(OpType.Create).refresh(Refresh.False)
                    .document(source).build();
            return Mono.defer(() -> client.index(command)).thenReturn(ProductResponse.from(entity));
        });
    }

    public Mono<ProductResponse> replace(String id, ProductRequest request) {
        return execute(id, () -> repository.save(document(id, request)).map(ProductResponse::from));
    }

    public Mono<ProductResponse> get(String id) {
        return execute(id, () -> repository.findById(id)
                .switchIfEmpty(Mono.error(missingProduct())).map(ProductResponse::from));
    }

    public Mono<Void> delete(String id) {
        return execute(id, () -> operations.delete(id, index)
                .switchIfEmpty(Mono.error(missingProduct())).then());
    }

    private <T> Mono<T> execute(String id, Supplier<Mono<T>> operation) {
        return limit.within(() -> {
            if (id == null || !id.matches("[a-zA-Z0-9_-]{1,64}")) {
                return Mono.error(new ChapterException(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_ID"));
            }
            return Mono.defer(() -> operations.indexOps(index).exists())
                    .flatMap(exists -> exists ? Mono.defer(operation)
                            : Mono.error(new ChapterException(HttpStatus.SERVICE_UNAVAILABLE, "INDEX_NOT_READY")))
                    .timeout(properties.requestTimeout());
        });
    }

    private static ProductDocument document(String id, ProductRequest request) {
        return new ProductDocument(id, request.name(), request.category(), request.priceSatang(), request.stock(),
                request.available(), request.createdAt());
    }

    private static ChapterException missingProduct() {
        return new ChapterException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND");
    }
}
