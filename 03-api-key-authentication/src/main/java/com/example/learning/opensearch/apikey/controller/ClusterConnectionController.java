package com.example.learning.opensearch.apikey.controller;

import com.example.learning.opensearch.apikey.dto.ClusterHealthResponse;
import com.example.learning.opensearch.apikey.dto.ClusterInfoResponse;
import com.example.learning.opensearch.apikey.service.ClusterConnectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/spring-data/cluster")
public class ClusterConnectionController {

    private final ClusterConnectionService service;

    public ClusterConnectionController(ClusterConnectionService service) {
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
