package com.example.learning.opensearch.mappings.controller;

import com.example.learning.opensearch.mappings.dto.CreateProductSampleRequest;
import com.example.learning.opensearch.mappings.dto.ProductResponse;
import com.example.learning.opensearch.mappings.service.ProductSampleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/spring-data/mapping-examples/products")
public class ProductSampleController {

    private final ProductSampleService service;

    public ProductSampleController(ProductSampleService service) {
        this.service = service;
    }

    @PutMapping("/{id}")
    public Mono<ProductResponse> save(@PathVariable String id, @Valid @RequestBody CreateProductSampleRequest request) {
        return service.save(id, request);
    }

    @GetMapping("/{id}")
    public Mono<ProductResponse> get(@PathVariable String id) {
        return service.get(id);
    }
}
