package com.example.learning.opensearch.mappings.service;

import com.example.learning.opensearch.mappings.config.LearningProperties;
import com.example.learning.opensearch.mappings.domain.ProductDocument;
import com.example.learning.opensearch.mappings.dto.CreateProductSampleRequest;
import com.example.learning.opensearch.mappings.exception.ChapterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductServicesTest {

    private final ReactiveElasticsearchOperations operations = mock(ReactiveElasticsearchOperations.class);
    private final ReactiveIndexOperations indexOperations = mock(ReactiveIndexOperations.class);
    private ProductIndexService indices;
    private ProductSampleService samples;
    private final IndexCoordinates coordinates = IndexCoordinates.of("ch04-unit");
    private final CreateProductSampleRequest request = new CreateProductSampleRequest("Keyboard", "electronics",
            129900L, 10, true, Instant.parse("2026-01-01T00:00:00Z"));

    @BeforeEach
    void setup() throws Exception {
        var properties = new LearningProperties("ch04-unit", Duration.ofSeconds(5));
        indices = new ProductIndexService(operations, mock(ReactiveOpenSearchClient.class), properties);
        samples = new ProductSampleService(operations, indices, properties);
        when(operations.indexOps(coordinates)).thenReturn(indexOperations);
    }

    @Test
    void createIsColdAndRetryCreatesFreshOperation() {
        when(indexOperations.createMapping(ProductDocument.class)).thenReturn(Mono.just(Document.create()));
        when(indexOperations.create(any(), any())).thenReturn(Mono.error(new IllegalStateException("retry")), Mono.just(true));
        var result = indices.create();
        verifyNoInteractions(indexOperations);
        StepVerifier.create(result.retry(1)).assertNext(value -> assertThat(value.acknowledged()).isTrue()).verifyComplete();
        verify(indexOperations, times(2)).createMapping(ProductDocument.class);
        verify(indexOperations, times(2)).create(any(), any());
    }

    @Test
    void saveBeforeCreateDoesNotWriteOrAutoCreate() {
        when(indexOperations.exists()).thenReturn(Mono.just(false));
        StepVerifier.create(samples.save("p-001", request)).expectErrorSatisfies(error ->
                assertThat(((ChapterException) error).code()).isEqualTo("INDEX_NOT_READY")).verify();
        verify(operations, never()).save(any(ProductDocument.class), any(IndexCoordinates.class));
    }

    @Test
    void saveIsColdAndPreservesIdentityAndUtcTimestamp() {
        when(indexOperations.exists()).thenReturn(Mono.just(true));
        when(operations.save(any(ProductDocument.class), eq(coordinates))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        var result = samples.save("p-001", request);
        verifyNoInteractions(indexOperations);
        StepVerifier.create(result).assertNext(value -> {
            assertThat(value.id()).isEqualTo("p-001");
            assertThat(value.priceSatang()).isEqualTo(129900L);
            assertThat(value.createdAt()).isEqualTo(request.createdAt());
        }).verifyComplete();
    }

    @Test
    void invalidIdNeverCallsBackend() {
        StepVerifier.create(samples.get("bad id")).expectErrorSatisfies(error ->
                assertThat(((ChapterException) error).code()).isEqualTo("INVALID_PRODUCT_ID")).verify();
        verifyNoInteractions(indexOperations);
    }

    @Test
    void missingDocumentAndMissingIndexHaveDistinctCodes() {
        when(indexOperations.exists()).thenReturn(Mono.just(true), Mono.just(false));
        when(operations.get("missing", ProductDocument.class, coordinates)).thenReturn(Mono.empty());
        StepVerifier.create(samples.get("missing")).expectErrorSatisfies(error ->
                assertThat(((ChapterException) error).code()).isEqualTo("PRODUCT_NOT_FOUND")).verify();
        StepVerifier.create(samples.get("missing")).expectErrorSatisfies(error ->
                assertThat(((ChapterException) error).code()).isEqualTo("INDEX_NOT_FOUND")).verify();
    }

    @Test
    void cancellationPropagatesToReactiveOperation() {
        when(indexOperations.exists()).thenReturn(Mono.just(true));
        var publisher = TestPublisher.<ProductDocument>create();
        when(operations.get("p-001", ProductDocument.class, coordinates)).thenReturn(publisher.mono());
        StepVerifier.create(samples.get("p-001")).then(publisher::assertSubscribers).thenCancel().verify();
        publisher.assertCancelled();
    }

    @Test
    void deadlineCancelsPendingOperation() {
        when(indexOperations.exists()).thenReturn(Mono.just(true));
        var publisher = TestPublisher.<ProductDocument>create();
        when(operations.get("p-001", ProductDocument.class, coordinates)).thenReturn(publisher.mono());
        StepVerifier.withVirtualTime(() -> samples.get("p-001")).thenAwait(Duration.ofSeconds(5))
                .expectError(TimeoutException.class).verify();
        publisher.assertCancelled();
    }

    @Test
    void synchronousDependencyFailureIsAReactiveError() {
        when(indexOperations.exists()).thenThrow(new IllegalStateException("sync test-only"));
        StepVerifier.create(indices.requireIndex(false)).expectError(IllegalStateException.class).verify();
    }

    @Test
    void unacknowledgedCreateAndAbsentDeleteAreNotSuccess() {
        when(indexOperations.createMapping(ProductDocument.class)).thenReturn(Mono.just(Document.create()));
        when(indexOperations.create(any(), any())).thenReturn(Mono.just(false));
        StepVerifier.create(indices.create()).expectErrorSatisfies(error ->
                assertThat(((ChapterException) error).code()).isEqualTo("INDEX_NOT_ACKNOWLEDGED")).verify();
        when(indexOperations.delete()).thenReturn(Mono.just(false));
        StepVerifier.create(indices.delete()).expectErrorSatisfies(error ->
                assertThat(((ChapterException) error).code()).isEqualTo("INDEX_NOT_FOUND")).verify();
    }
}
