package com.example.learning.opensearch.basicconnection.controller;

import com.example.learning.opensearch.basicconnection.dto.ClusterHealthResponse;
import com.example.learning.opensearch.basicconnection.dto.ClusterInfoResponse;
import com.example.learning.opensearch.basicconnection.service.JavaClientClusterConnectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping({"/api/java-client/cluster", "/api/cluster"})
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
