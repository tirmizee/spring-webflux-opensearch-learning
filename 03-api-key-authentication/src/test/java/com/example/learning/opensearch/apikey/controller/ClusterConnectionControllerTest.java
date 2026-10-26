package com.example.learning.opensearch.apikey.controller;

import com.example.learning.opensearch.apikey.dto.ClusterInfoResponse;
import com.example.learning.opensearch.apikey.exception.ApiExceptionHandler;
import com.example.learning.opensearch.apikey.service.ClusterConnectionService;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClusterConnectionControllerTest {

    private final ClusterConnectionService service = mock(ClusterConnectionService.class);
    private final WebTestClient http = WebTestClient.bindToController(new ClusterConnectionController(service))
            .controllerAdvice(new ApiExceptionHandler()).build();
    private static final String PREFIX = "/api/spring-data/cluster";

    @Test
    void infoUsesPublicContract() {
        when(service.info()).thenReturn(Mono.just(new ClusterInfoResponse("cluster", "uuid", "node", "opensearch", "3.9.0")));
        http.get().uri(PREFIX + "/info").exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.clusterName").isEqualTo("cluster").jsonPath("$.clusterUuid").isEqualTo("uuid")
                .jsonPath("$.nodeName").isEqualTo("node").jsonPath("$.distribution").isEqualTo("opensearch")
                .jsonPath("$.version").isEqualTo("3.9.0");
    }

    @Test
    void mapsDependencyFailuresWithoutExposingToken() {
        for (int status : new int[]{401, 403}) {
            var failure = new OpenSearchException(new ErrorResponse.Builder().status(status)
                    .error(error -> error.type("security_exception").reason("private-token")).build());
            when(service.info()).thenReturn(Mono.error(failure));
            http.get().uri(PREFIX + "/info").exchange().expectStatus().isEqualTo(502).expectBody()
                    .jsonPath("$.code").isEqualTo(status == 401 ? "OPENSEARCH_AUTHENTICATION_FAILED" : "OPENSEARCH_AUTHORIZATION_FAILED")
                    .jsonPath("$.detail").isEqualTo("The OpenSearch dependency could not serve this request.");
        }
        when(service.info()).thenReturn(Mono.error(new IOException("private-token")));
        http.get().uri(PREFIX + "/info").exchange().expectStatus().isEqualTo(503);
        when(service.health()).thenReturn(Mono.error(new TimeoutException()));
        http.get().uri(PREFIX + "/health").exchange().expectStatus().isEqualTo(504);
    }
}
