package com.example.learning.opensearch.documentcrud.repository;

import com.example.learning.opensearch.documentcrud.domain.ProductDocument;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;

public interface ProductRepository extends ReactiveElasticsearchRepository<ProductDocument, String> {
}
