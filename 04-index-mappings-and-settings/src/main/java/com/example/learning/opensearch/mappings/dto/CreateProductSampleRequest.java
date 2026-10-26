package com.example.learning.opensearch.mappings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateProductSampleRequest(
    @NotBlank @Size(max = 256) String name,
    @NotBlank @Size(max = 80) String category,
    @NotNull @PositiveOrZero Long priceSatang,
    @NotNull @PositiveOrZero Integer stock,
    @NotNull Boolean available,
    @NotNull Instant createdAt
) {
}
