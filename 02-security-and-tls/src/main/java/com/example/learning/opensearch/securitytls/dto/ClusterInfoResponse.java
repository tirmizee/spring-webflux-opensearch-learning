package com.example.learning.opensearch.securitytls.dto;

public record ClusterInfoResponse(
    String clusterName,
    String clusterUuid,
    String nodeName,
    String distribution,
    String version
) {
}
