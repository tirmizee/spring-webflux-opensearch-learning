package com.example.learning.opensearch.documentcrud.controller;

import com.example.learning.opensearch.documentcrud.dto.ProductRequest;
import com.example.learning.opensearch.documentcrud.dto.ProductResponse;
import com.example.learning.opensearch.documentcrud.exception.ApiExceptionHandler;
import com.example.learning.opensearch.documentcrud.exception.ChapterException;
import com.example.learning.opensearch.documentcrud.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

class ProductControllerTest {

    private final ProductService service = mock(ProductService.class);
    private final String api = "/api/spring-data/products/p-001";
    private final Map<String, Object> sample = Map.of("name", "Keyboard", "category", "electronics", "priceSatang", 129900,
            "stock", 10, "available", true, "createdAt", "2026-01-01T00:00:00Z");
    private final ProductResponse response = new ProductResponse("p-001", "Keyboard", "electronics", 129900L, 10, true,
            Instant.parse("2026-01-01T00:00:00Z"));
    private WebTestClient http;

    @BeforeEach
    void setup() {
        http = WebTestClient.bindToController(new ProductController(service)).controllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void allCrudRoutesReturnExpectedStatusAndLocation() {
        when(service.create(eq("p-001"), any(ProductRequest.class))).thenReturn(Mono.just(response));
        when(service.replace(eq("p-001"), any(ProductRequest.class))).thenReturn(Mono.just(response));
        when(service.get("p-001")).thenReturn(Mono.just(response));
        when(service.delete("p-001")).thenReturn(Mono.empty());
        http.post().uri(api).bodyValue(sample).exchange().expectStatus().isCreated()
                .expectHeader().valueEquals("Location", api).expectBody().jsonPath("$.id").isEqualTo("p-001");
        http.put().uri(api).bodyValue(sample).exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.createdAt").isEqualTo("2026-01-01T00:00:00Z");
        http.get().uri(api).exchange().expectStatus().isOk();
        http.delete().uri(api).exchange().expectStatus().isNoContent().expectBody().isEmpty();
    }

    @Test
    void incompleteAndNegativeBodiesAreRejectedBeforeService() {
        http.post().uri(api).bodyValue(Map.of("name", " ")).exchange().expectStatus().isBadRequest();
        var invalid = new java.util.HashMap<>(sample);
        invalid.put("stock", -1);
        http.put().uri(api).bodyValue(invalid).exchange().expectStatus().isBadRequest();
        verifyNoInteractions(service);
    }

    @Test
    void productErrorsExposeCodesWithoutRawDetails() {
        when(service.get("p-001")).thenReturn(Mono.error(new ChapterException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND")));
        http.get().uri(api).exchange().expectStatus().isNotFound().expectBody()
                .jsonPath("$.code").isEqualTo("PRODUCT_NOT_FOUND")
                .jsonPath("$.detail").isEqualTo("The chapter operation could not be completed.");
    }
}
