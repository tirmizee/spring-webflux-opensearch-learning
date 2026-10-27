package com.example.learning.opensearch.documentcrud.controller;

import com.example.learning.opensearch.documentcrud.dto.ProductRequest;
import com.example.learning.opensearch.documentcrud.dto.ProductResponse;
import com.example.learning.opensearch.documentcrud.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequestMapping("/api/spring-data/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @PostMapping("/{id}")
    public Mono<ResponseEntity<ProductResponse>> create(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        return service.create(id, request)
                .map(product -> ResponseEntity.created(URI.create("/api/spring-data/products/" + product.id())).body(product));
    }

    @PutMapping("/{id}")
    public Mono<ProductResponse> replace(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        return service.replace(id, request);
    }

    @GetMapping("/{id}")
    public Mono<ProductResponse> get(@PathVariable String id) {
        return service.get(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable String id) {
        return service.delete(id);
    }
}
