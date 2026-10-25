package com.example.learning.opensearch.basicconnection.dto;

public record ClusterInfoResponse(
    String clusterName,
    String clusterUuid,
    String nodeName,
    String distribution,
    String version
) {
}
