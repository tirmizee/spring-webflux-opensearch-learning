package com.example.learning.opensearch.documentcrud.dto;

import com.example.learning.opensearch.documentcrud.domain.ProductDocument;
import java.time.Instant;

public record ProductResponse(
    String id,
    String name,
    String category,
    Long priceSatang,
    Integer stock,
    Boolean available,
    Instant createdAt
) {

    public static ProductResponse from(ProductDocument product) {
        return new ProductResponse(product.id(), product.name(), product.category(), product.priceSatang(),
                product.stock(), product.available(), product.createdAt());
    }
}
