package com.example.learning.opensearch.basicconnection.dto;

public record ClusterHealthResponse(
    String clusterName,
    String status,
    int numberOfNodes,
    int numberOfDataNodes,
    boolean timedOut
) {
}
