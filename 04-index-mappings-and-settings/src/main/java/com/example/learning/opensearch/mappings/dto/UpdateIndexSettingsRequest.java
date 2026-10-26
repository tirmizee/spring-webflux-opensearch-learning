package com.example.learning.opensearch.mappings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateIndexSettingsRequest(@NotNull @Min(0) @Max(1) Integer numberOfReplicas, @NotNull @Pattern(regexp = "1s|2s|5s") String refreshInterval) {
}
