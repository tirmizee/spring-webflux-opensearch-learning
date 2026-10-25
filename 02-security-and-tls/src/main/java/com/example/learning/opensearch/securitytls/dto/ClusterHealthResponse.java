package com.example.learning.opensearch.securitytls.dto;

public record ClusterHealthResponse(
    String clusterName,
    String status,
    int numberOfNodes,
    int numberOfDataNodes,
    boolean timedOut
) {
}
