package com.example.learning.opensearch.mappings.dto;

public record IndexSettingsResponse(
    String indexName,
    int numberOfShards,
    int numberOfReplicas,
    String refreshInterval,
    boolean coerce
) {
}
