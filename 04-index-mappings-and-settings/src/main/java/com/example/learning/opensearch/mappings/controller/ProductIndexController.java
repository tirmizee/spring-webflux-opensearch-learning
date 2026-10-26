package com.example.learning.opensearch.mappings.controller;

import com.example.learning.opensearch.mappings.dto.IndexMappingResponse;
import com.example.learning.opensearch.mappings.dto.IndexOperationResponse;
import com.example.learning.opensearch.mappings.dto.IndexSettingsResponse;
import com.example.learning.opensearch.mappings.dto.UpdateIndexSettingsRequest;
import com.example.learning.opensearch.mappings.service.ProductIndexService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/spring-data/indices/products")
public class ProductIndexController {

    private final ProductIndexService service;

    public ProductIndexController(ProductIndexService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<IndexOperationResponse> create() {
        return service.create();
    }

    @GetMapping("/mapping")
    public Mono<IndexMappingResponse> mapping() {
        return service.mapping();
    }

    @GetMapping("/settings")
    public Mono<IndexSettingsResponse> settings() {
        return service.settings();
    }

    @PatchMapping("/settings")
    public Mono<IndexSettingsResponse> updateSettings(@Valid @RequestBody UpdateIndexSettingsRequest request) {
        return service.updateSettings(request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete() {
        return service.delete();
    }
}
