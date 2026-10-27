package com.example.learning.opensearch.documentcrud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ProductRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 64) String category,
    @NotNull @PositiveOrZero Long priceSatang,
    @NotNull @PositiveOrZero Integer stock,
    @NotNull Boolean available,
    @NotNull Instant createdAt
) {
}
