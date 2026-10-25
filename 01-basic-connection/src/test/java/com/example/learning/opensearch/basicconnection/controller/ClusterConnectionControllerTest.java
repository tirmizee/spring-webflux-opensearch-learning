package com.example.learning.opensearch.basicconnection.controller;

import com.example.learning.opensearch.basicconnection.dto.ClusterHealthResponse;
import com.example.learning.opensearch.basicconnection.dto.ClusterInfoResponse;
import com.example.learning.opensearch.basicconnection.exception.ApiExceptionHandler;
import com.example.learning.opensearch.basicconnection.service.JavaClientClusterConnectionService;
import com.example.learning.opensearch.basicconnection.service.SpringDataClusterConnectionService;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest({JavaClientClusterConnectionController.class, SpringDataClusterConnectionController.class})
@Import(ApiExceptionHandler.class)
class ClusterConnectionControllerTest {

    @Autowired
    private WebTestClient http;
    @MockitoBean
    private JavaClientClusterConnectionService javaClient;
    @MockitoBean
    private SpringDataClusterConnectionService springData;

    @ParameterizedTest
    @ValueSource(strings = {"/api/java-client/cluster", "/api/spring-data/cluster", "/api/cluster"})
    void infoReturnsSharedPublicContractAndUsesCorrectService(String prefix) {
        stubInfo(prefix, Mono.just(new ClusterInfoResponse(
                "test-cluster", "uuid", "node", "opensearch", "3.9.0"
        )));
        http.get().uri(prefix + "/info").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.clusterName").isEqualTo("test-cluster")
                .jsonPath("$.clusterUuid").isEqualTo("uuid")
                .jsonPath("$.nodeName").isEqualTo("node")
                .jsonPath("$.version").isEqualTo("3.9.0")
                .jsonPath("$.distribution").isEqualTo("opensearch");
        verifyOtherServiceUnused(prefix);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/java-client/cluster", "/api/spring-data/cluster", "/api/cluster"})
    void yellowClusterIsReachableAndReturnedAsData(String prefix) {
        stubHealth(prefix, Mono.just(new ClusterHealthResponse("test-cluster", "yellow", 1, 1, false)));
        http.get().uri(prefix + "/health").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("yellow")
                .jsonPath("$.numberOfNodes").isEqualTo(1)
                .jsonPath("$.numberOfDataNodes").isEqualTo(1)
                .jsonPath("$.timedOut").isEqualTo(false);
        verifyOtherServiceUnused(prefix);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/java-client/cluster", "/api/spring-data/cluster", "/api/cluster"})
    void connectionFailureIsSanitized503OnBothEndpoints(String prefix) {
        stubInfo(prefix, Mono.error(new IOException("secret-host-and-credential")));
        stubHealth(prefix, Mono.error(new IOException("secret-host-and-credential")));
        for (String endpoint : new String[]{"info", "health"}) {
            http.get().uri(prefix + "/" + endpoint).exchange().expectStatus().isEqualTo(503)
                    .expectBody().jsonPath("$.status").isEqualTo(503)
                    .jsonPath("$.title").isEqualTo("OpenSearch unavailable")
                    .jsonPath("$.detail").isEqualTo("The OpenSearch dependency could not serve this request.");
        }
        verifyOtherServiceUnused(prefix);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/java-client/cluster", "/api/spring-data/cluster", "/api/cluster"})
    void upstreamSecurityFailureIsNotCaller401(String prefix) {
        var failure = new OpenSearchException(new ErrorResponse.Builder()
                .status(401)
                .error(error -> error.type("security_exception").reason("private credential detail"))
                .build());
        stubInfo(prefix, Mono.error(failure));
        http.get().uri(prefix + "/info").exchange().expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.title").isEqualTo("OpenSearch unavailable");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/java-client/cluster", "/api/spring-data/cluster", "/api/cluster"})
    void reactorDeadlineIs504(String prefix) {
        stubInfo(prefix, Mono.error(new TimeoutException("private")));
        http.get().uri(prefix + "/info").exchange().expectStatus().isEqualTo(504)
                .expectBody().jsonPath("$.title").isEqualTo("OpenSearch timeout");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/java-client/cluster", "/api/spring-data/cluster", "/api/cluster"})
    void transportTimeoutIs504(String prefix) {
        stubHealth(prefix, Mono.error(new SocketTimeoutException("private")));
        http.get().uri(prefix + "/health").exchange().expectStatus().isEqualTo(504)
                .expectBody().jsonPath("$.status").isEqualTo(504);
    }

    private void stubInfo(String prefix, Mono<ClusterInfoResponse> result) {
        if (prefix.contains("spring-data")) {
            when(springData.info()).thenReturn(result);
        } else {
            when(javaClient.info()).thenReturn(result);
        }
    }

    private void stubHealth(String prefix, Mono<ClusterHealthResponse> result) {
        if (prefix.contains("spring-data")) {
            when(springData.health()).thenReturn(result);
        } else {
            when(javaClient.health()).thenReturn(result);
        }
    }

    private void verifyOtherServiceUnused(String prefix) {
        if (prefix.contains("spring-data")) {
            verifyNoInteractions(javaClient);
        } else {
            verifyNoInteractions(springData);
        }
    }
}
