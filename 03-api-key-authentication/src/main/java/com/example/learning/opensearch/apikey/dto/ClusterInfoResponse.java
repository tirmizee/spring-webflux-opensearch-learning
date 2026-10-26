package com.example.learning.opensearch.apikey.dto;

public record ClusterInfoResponse(
    String clusterName,
    String clusterUuid,
    String nodeName,
    String distribution,
    String version
) {
}
