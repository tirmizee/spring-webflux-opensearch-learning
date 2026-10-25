package com.example.learning.opensearch.securitytls.controller;

import com.example.learning.opensearch.securitytls.dto.ClusterHealthResponse;
import com.example.learning.opensearch.securitytls.dto.ClusterInfoResponse;
import com.example.learning.opensearch.securitytls.service.JavaClientClusterConnectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/java-client/cluster")
public class JavaClientClusterConnectionController {

    private final JavaClientClusterConnectionService service;

    public JavaClientClusterConnectionController(JavaClientClusterConnectionService service) {
        this.service = service;
    }

    @GetMapping("/info")
    public Mono<ClusterInfoResponse> info() {
        return service.info();
    }

    @GetMapping("/health")
    public Mono<ClusterHealthResponse> health() {
        return service.health();
    }
}
