package com.example.learning.opensearch.securitytls.service;

import com.example.learning.opensearch.securitytls.config.OpenSearchProperties;
import com.example.learning.opensearch.securitytls.dto.ClusterHealthResponse;
import com.example.learning.opensearch.securitytls.dto.ClusterInfoResponse;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.opensearch.cluster.HealthRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
public class JavaClientClusterConnectionService {

    private final OpenSearchAsyncClient client;
    private final OpenSearchProperties properties;

    public JavaClientClusterConnectionService(OpenSearchAsyncClient client, OpenSearchProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public Mono<ClusterInfoResponse> info() {
        return call(client::info)
                .map(info -> new ClusterInfoResponse(
                        info.clusterName(),
                        info.clusterUuid(),
                        info.name(),
                        info.version().distribution(),
                        info.version().number()
                ));
    }

    public Mono<ClusterHealthResponse> health() {
        return call(() -> client.cluster().health(new HealthRequest.Builder().build()))
                .map(health -> new ClusterHealthResponse(
                        health.clusterName(),
                        health.status().jsonValue(),
                        health.numberOfNodes(),
                        health.numberOfDataNodes(),
                        health.timedOut()
                ));
    }

    private <T> Mono<T> call(AsyncCall<T> operation) {
        return Mono.defer(() -> {
            try {
                return Mono.fromFuture(operation.execute());
            } catch (IOException exception) {
                return Mono.error(exception);
            }
        }).timeout(properties.requestTimeout());
    }

    // ตรวจ checked IOException ที่อาจเกิดก่อน client คืน future โดยไม่ซ่อนด้วย SneakyThrows
    @FunctionalInterface
    private interface AsyncCall<T> {
        CompletableFuture<T> execute() throws IOException;
    }
}
