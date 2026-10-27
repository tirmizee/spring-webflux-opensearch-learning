package com.example.learning.opensearch.documentcrud.service;

import com.example.learning.opensearch.documentcrud.config.LearningProperties;
import com.example.learning.opensearch.documentcrud.domain.ProductDocument;
import com.example.learning.opensearch.documentcrud.dto.ProductRequest;
import com.example.learning.opensearch.documentcrud.exception.ChapterException;
import com.example.learning.opensearch.documentcrud.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opensearch.client.opensearch._types.OpType;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

class ProductServiceTest {

    private final ProductRepository repository = mock(ProductRepository.class);
    private final ReactiveElasticsearchOperations operations = mock(ReactiveElasticsearchOperations.class);
    private final ReactiveIndexOperations indices = mock(ReactiveIndexOperations.class);
    private final ReactiveOpenSearchClient client = mock(ReactiveOpenSearchClient.class);
    private final ElasticsearchConverter converter = mock(ElasticsearchConverter.class);
    private final ProductRequest request = new ProductRequest("Keyboard", "electronics", 129900L, 10, true,
            Instant.parse("2026-01-01T00:00:00Z"));
    private final ProductDocument entity = new ProductDocument("p-001", "Keyboard", "electronics", 129900L, 10, true,
            Instant.parse("2026-01-01T00:00:00Z"));
    private ProductService service;

    @BeforeEach
    void setup() {
        service = new ProductService(repository, operations, client,
                new LearningProperties("ch05-unit", Duration.ofSeconds(5)), new RequestLimit());
        when(operations.indexOps(any(IndexCoordinates.class))).thenReturn(indices);
        when(indices.exists()).thenReturn(Mono.just(true));
    }

    private Mono<?> invoke(String action) {
        return switch (action) {
            case "create" -> service.create("p-001", request);
            case "replace" -> service.replace("p-001", request);
            case "get" -> service.get("p-001");
            case "delete" -> service.delete("p-001");
            default -> throw new IllegalArgumentException(action);
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"create", "replace", "get", "delete"})
    void allMethodsAreColdAndReinvokeOnRetry(String action) {
        when(indices.exists()).thenReturn(Mono.error(new IllegalStateException("transient")), Mono.just(false));
        var result = invoke(action);
        verify(indices, never()).exists();
        verifyNoInteractions(repository, client);
        StepVerifier.create(result.retry(1)).expectErrorSatisfies(error ->
                assertThat(((ChapterException) error).code()).isEqualTo("INDEX_NOT_READY")).verify();
        verify(indices, times(2)).exists();
        verifyNoInteractions(repository, client);
    }

    @Test
    void createDefersClientAndUsesAtomicCreateWithConvertedSource() {
        when(operations.getElasticsearchConverter()).thenReturn(converter);
        var source = Document.from(java.util.Map.of("name", "Keyboard"));
        when(converter.mapObject(any(ProductDocument.class))).thenReturn(source);
        when(client.index(any(IndexRequest.class))).thenAnswer(invocation -> {
            IndexRequest<?> command = invocation.getArgument(0);
            assertThat(command.opType()).isEqualTo(OpType.Create);
            assertThat(command.index()).isEqualTo("ch05-unit");
            assertThat(command.id()).isEqualTo("p-001");
            assertThat(command.document()).isSameAs(source);
            return Mono.empty();
        });
        var result = service.create("p-001", request);
        verifyNoInteractions(client, converter);
        StepVerifier.create(result).assertNext(value -> assertThat(value.id()).isEqualTo("p-001")).verifyComplete();
        verifyNoInteractions(repository);
    }

    @Test
    void retryCreatesFreshClientInvocationIncludingSynchronousFailure() {
        when(operations.getElasticsearchConverter()).thenReturn(converter);
        when(converter.mapObject(any(ProductDocument.class))).thenReturn(Document.create());
        when(client.index(any(IndexRequest.class))).thenThrow(new IllegalStateException("sync"))
                .thenReturn(Mono.empty());
        StepVerifier.create(service.create("p-001", request).retry(1)).expectNextCount(1).verifyComplete();
        verify(client, times(2)).index(any(IndexRequest.class));
        verify(converter, times(2)).mapObject(any(ProductDocument.class));
    }

    @Test
    void replaceAndGetUseRepositoryWithIdAndUtcTimestamp() {
        when(repository.save(any(ProductDocument.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));
        when(repository.findById("p-001")).thenReturn(Mono.just(entity));
        StepVerifier.create(service.replace("p-001", request)).assertNext(value -> {
            assertThat(value.id()).isEqualTo("p-001");
            assertThat(value.createdAt()).isEqualTo(request.createdAt());
            assertThat(value.priceSatang()).isEqualTo(129900L);
        }).verifyComplete();
        StepVerifier.create(service.get("p-001")).assertNext(value -> assertThat(value.name()).isEqualTo("Keyboard"))
                .verifyComplete();
        verifyNoInteractions(client);
    }

    @Test
    void repositoryAndDeleteRetryInvokeFreshOperations() {
        when(repository.save(any(ProductDocument.class)))
                .thenReturn(Mono.error(new IllegalStateException("first save")), Mono.just(entity));
        when(repository.findById("p-001"))
                .thenReturn(Mono.error(new IllegalStateException("first get")), Mono.just(entity));
        when(operations.delete(eq("p-001"), any(IndexCoordinates.class)))
                .thenReturn(Mono.error(new IllegalStateException("first delete")), Mono.just("p-001"));
        var save = service.replace("p-001", request);
        var get = service.get("p-001");
        var delete = service.delete("p-001");
        verify(repository, never()).save(any(ProductDocument.class));
        verify(repository, never()).findById("p-001");
        verify(operations, never()).delete(eq("p-001"), any(IndexCoordinates.class));
        StepVerifier.create(save.retry(1)).expectNextCount(1).verifyComplete();
        StepVerifier.create(get.retry(1)).expectNextCount(1).verifyComplete();
        StepVerifier.create(delete.retry(1)).verifyComplete();
        verify(repository, times(2)).save(any(ProductDocument.class));
        verify(repository, times(2)).findById("p-001");
        verify(operations, times(2)).delete(eq("p-001"), any(IndexCoordinates.class));
    }

    @Test
    void repositoryAsyncFailureAndSyncThrowRemainReactiveErrors() {
        when(repository.findById("p-001")).thenReturn(Mono.error(new IllegalStateException("async")))
                .thenThrow(new IllegalArgumentException("sync"));
        StepVerifier.create(service.get("p-001")).expectErrorMessage("async").verify();
        StepVerifier.create(service.get("p-001")).expectErrorMessage("sync").verify();
    }

    @Test
    void invalidIdNeverCallsBackend() {
        StepVerifier.create(service.get("bad id")).expectErrorSatisfies(error ->
                assertThat(((ChapterException) error).code()).isEqualTo("INVALID_PRODUCT_ID")).verify();
        verify(indices, never()).exists();
        verifyNoInteractions(repository, client);
    }

    @Test
    void absentGetAndDeleteMapToMissingProductWithoutExistsThenDeleteRace() {
        when(repository.findById("p-001")).thenReturn(Mono.empty());
        when(operations.delete(eq("p-001"), any(IndexCoordinates.class))).thenReturn(Mono.empty());
        for (var result : new Mono<?>[]{service.get("p-001"), service.delete("p-001")}) {
            StepVerifier.create(result).expectErrorSatisfies(error ->
                    assertThat(((ChapterException) error).code()).isEqualTo("PRODUCT_NOT_FOUND")).verify();
        }
        verify(repository, never()).existsById(any(String.class));
    }

    @Test
    void deleteSuccessCompletesWithoutPayload() {
        when(operations.delete(eq("p-001"), any(IndexCoordinates.class))).thenReturn(Mono.just("p-001"));
        StepVerifier.create(service.delete("p-001")).verifyComplete();
    }

    @Test
    void cancellationPropagatesToRepositoryPublisher() {
        var pending = TestPublisher.<ProductDocument>create();
        when(repository.findById("p-001")).thenReturn(pending.mono());
        StepVerifier.create(service.get("p-001")).then(pending::assertSubscribers).thenCancel().verify();
        pending.assertCancelled();
    }

    @Test
    void deadlineCancelsClientPublisher() {
        when(operations.getElasticsearchConverter()).thenReturn(converter);
        when(converter.mapObject(any(ProductDocument.class))).thenReturn(Document.create());
        var pending = TestPublisher.<IndexResponse>create();
        when(client.index(any(IndexRequest.class))).thenReturn(pending.mono());
        StepVerifier.withVirtualTime(() -> service.create("p-001", request)).thenAwait(Duration.ofSeconds(5))
                .expectError(TimeoutException.class).verify();
        pending.assertCancelled();
    }
}
