package com.example.learning.opensearch.apikey.service;

import com.example.learning.opensearch.apikey.config.ApiKeyConnectionProperties;
import com.example.learning.opensearch.apikey.dto.ClusterHealthResponse;
import com.example.learning.opensearch.apikey.dto.ClusterInfoResponse;
import org.opensearch.client.opensearch.cluster.HealthRequest;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ClusterConnectionService {

    private final ReactiveOpenSearchClient client;
    private final ApiKeyConnectionProperties properties;

    public ClusterConnectionService(ReactiveOpenSearchClient client, ApiKeyConnectionProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public Mono<ClusterInfoResponse> info() {
        // client.info() เริ่ม I/O ก่อนคืน Mono ใน version นี้ จึง defer ทั้ง invocation
        return Mono.defer(client::info)
                .timeout(properties.requestTimeout())
                .map(info -> new ClusterInfoResponse(
                        info.clusterName(),
                        info.clusterUuid(),
                        info.name(),
                        info.version().distribution(),
                        info.version().number()
                ));
    }

    public Mono<ClusterHealthResponse> health() {
        return Mono.defer(() -> client.cluster().health(new HealthRequest.Builder().build()))
                .timeout(properties.requestTimeout())
                .map(health -> new ClusterHealthResponse(
                        health.clusterName(),
                        health.status().jsonValue(),
                        health.numberOfNodes(),
                        health.numberOfDataNodes(),
                        health.timedOut()
                ));
    }
}
