package com.example.learning.opensearch.mappings.controller;

import com.example.learning.opensearch.mappings.dto.IndexMappingResponse;
import com.example.learning.opensearch.mappings.dto.IndexOperationResponse;
import com.example.learning.opensearch.mappings.dto.IndexSettingsResponse;
import com.example.learning.opensearch.mappings.dto.ProductResponse;
import com.example.learning.opensearch.mappings.dto.UpdateIndexSettingsRequest;
import com.example.learning.opensearch.mappings.exception.ApiExceptionHandler;
import com.example.learning.opensearch.mappings.exception.ChapterException;
import com.example.learning.opensearch.mappings.service.ProductIndexService;
import com.example.learning.opensearch.mappings.service.ProductSampleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductControllerTest {

    private final ProductIndexService indices = mock(ProductIndexService.class);
    private final ProductSampleService products = mock(ProductSampleService.class);
    private WebTestClient http;
    private static final String INDEX = "/api/spring-data/indices/products";
    private static final String SAMPLE = "/api/spring-data/mapping-examples/products/p-001";
    private static final Map<String, Object> VALID = Map.of("name", "Keyboard", "category", "electronics",
            "priceSatang", 129900, "stock", 10, "available", true, "createdAt", "2026-01-01T00:00:00Z");

    @BeforeEach
    void setup() {
        http = WebTestClient.bindToController(new ProductIndexController(indices), new ProductSampleController(products))
                .controllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void allIndexEndpointsHaveExpectedStatusAndPayload() {
        when(indices.create()).thenReturn(Mono.just(new IndexOperationResponse("ch04-products", true)));
        when(indices.mapping()).thenReturn(Mono.just(new IndexMappingResponse("ch04-products", Map.of("dynamic", "strict"))));
        var settings = new IndexSettingsResponse("ch04-products", 1, 0, "1s", false);
        when(indices.settings()).thenReturn(Mono.just(settings));
        when(indices.updateSettings(new UpdateIndexSettingsRequest(0, "2s"))).thenReturn(Mono.just(settings));
        when(indices.delete()).thenReturn(Mono.empty());
        http.post().uri(INDEX).exchange().expectStatus().isCreated().expectBody().jsonPath("$.acknowledged").isEqualTo(true);
        http.get().uri(INDEX + "/mapping").exchange().expectStatus().isOk().expectBody().jsonPath("$.mapping.dynamic").isEqualTo("strict");
        http.get().uri(INDEX + "/settings").exchange().expectStatus().isOk().expectBody().jsonPath("$.coerce").isEqualTo(false);
        http.patch().uri(INDEX + "/settings").bodyValue(Map.of("numberOfReplicas", 0, "refreshInterval", "2s"))
                .exchange().expectStatus().isOk();
        http.delete().uri(INDEX).exchange().expectStatus().isNoContent().expectBody().isEmpty();
    }

    @Test
    void sampleWriteAndReadReturnSameContract() {
        var response = new ProductResponse("p-001", "Keyboard", "electronics", 129900L, 10, true,
                Instant.parse("2026-01-01T00:00:00Z"));
        when(products.save(eq("p-001"), any())).thenReturn(Mono.just(response));
        when(products.get("p-001")).thenReturn(Mono.just(response));
        http.put().uri(SAMPLE).bodyValue(VALID).exchange().expectStatus().isOk().expectBody().jsonPath("$.id").isEqualTo("p-001");
        http.get().uri(SAMPLE).exchange().expectStatus().isOk().expectBody().jsonPath("$.priceSatang").isEqualTo(129900);
    }

    @Test
    void invalidBodiesAreRejectedBeforeService() {
        http.put().uri(SAMPLE).bodyValue(Map.of("name", " ")).exchange().expectStatus().isBadRequest();
        http.put().uri(SAMPLE).contentType(MediaType.APPLICATION_JSON).bodyValue("{broken")
                .exchange().expectStatus().isBadRequest();
        http.patch().uri(INDEX + "/settings").bodyValue(Map.of("numberOfReplicas", 2, "refreshInterval", "-1"))
                .exchange().expectStatus().isBadRequest();
        verifyNoInteractions(products, indices);
    }

    @Test
    void meaningfulFailuresHaveSanitizedCodes() {
        when(indices.mapping()).thenReturn(Mono.error(new ChapterException(HttpStatus.NOT_FOUND, "INDEX_NOT_FOUND")));
        when(products.get("p-001")).thenReturn(Mono.error(new TimeoutException("private transport detail")));
        http.get().uri(INDEX + "/mapping").exchange().expectStatus().isNotFound().expectBody()
                .jsonPath("$.code").isEqualTo("INDEX_NOT_FOUND");
        http.get().uri(SAMPLE).exchange().expectStatus().isEqualTo(504).expectBody()
                .jsonPath("$.code").isEqualTo("OPENSEARCH_TIMEOUT")
                .jsonPath("$.detail").isEqualTo("The chapter operation could not be completed.");
    }
}
