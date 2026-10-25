package com.example.learning.opensearch.securitytls.service;

import com.example.learning.opensearch.securitytls.config.OpenSearchProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch._types.OpenSearchVersionInfo;
import org.opensearch.client.opensearch.cluster.HealthRequest;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch.cluster.OpenSearchClusterAsyncClient;
import org.opensearch.client.opensearch.core.InfoResponse;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JavaClientClusterConnectionServiceTest {

    @Mock
    private OpenSearchAsyncClient client;
    private JavaClientClusterConnectionService service;

    @BeforeEach
    void setUp() {
        service = new JavaClientClusterConnectionService(client, new OpenSearchProperties(
                URI.create("https://localhost:9202"),
                java.nio.file.Path.of("unused-ca.pem"), "ch02_monitor", java.nio.file.Path.of("unused.password"),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofSeconds(3),
                Duration.ofSeconds(5)
        ));
    }

    @Test
    void infoIsLazyAndCallsAgainForEachSubscription() throws IOException {
        var response = infoResponse();
        when(client.info()).thenAnswer(invocation -> CompletableFuture.completedFuture(response));
        var result = service.info();
        verifyNoInteractions(client);
        for (int subscription = 0; subscription < 2; subscription++) {
            StepVerifier.create(result).assertNext(info -> {
                assertThat(info.clusterName()).isEqualTo("test-cluster");
                assertThat(info.clusterUuid()).isEqualTo("test-uuid");
                assertThat(info.nodeName()).isEqualTo("test-node");
                assertThat(info.distribution()).isEqualTo("opensearch");
                assertThat(info.version()).isEqualTo("3.9.0");
            }).verifyComplete();
        }
        verify(client, times(2)).info();
    }

    @Test
    void synchronousIOExceptionIsAnErrorSignal() throws IOException {
        var failure = new IOException("private connection detail");
        when(client.info()).thenThrow(failure);
        var result = service.info();
        verifyNoInteractions(client);
        StepVerifier.create(result).expectErrorMatches(error -> error == failure).verify();
    }

    @Test
    void futureFailureUnwrapsCompletionException() throws IOException {
        var failure = new IOException("upstream failure");
        when(client.info()).thenReturn(CompletableFuture.failedFuture(new CompletionException(failure)));
        StepVerifier.create(service.info()).expectErrorMatches(error -> error == failure).verify();
    }

    @Test
    void retryResubscribesToANewInvocation() throws IOException {
        var response = infoResponse();
        when(client.info())
                .thenReturn(CompletableFuture.failedFuture(new IOException("temporary")))
                .thenReturn(CompletableFuture.completedFuture(response));
        StepVerifier.create(service.info().retry(1)).expectNextCount(1).verifyComplete();
        verify(client, times(2)).info();
    }

    @Test
    void cancellationReachesFutureWithoutClaimingServerAbort() throws IOException {
        var future = new CompletableFuture<InfoResponse>();
        when(client.info()).thenReturn(future);
        StepVerifier.create(service.info()).thenCancel().verify();
        assertThat(future).isCancelled();
    }

    @Test
    void deadlineEmitsTimeoutAndCancelsPendingFuture() throws IOException {
        var future = new CompletableFuture<InfoResponse>();
        when(client.info()).thenReturn(future);
        StepVerifier.withVirtualTime(service::info)
                .thenAwait(Duration.ofSeconds(5))
                .expectError(TimeoutException.class)
                .verify();
        assertThat(future).isCancelled();
    }

    @Test
    void healthIsLazyAndPreservesYellowAndTimedOut() throws IOException {
        var cluster = mock(OpenSearchClusterAsyncClient.class);
        var response = mock(HealthResponse.class);
        when(client.cluster()).thenReturn(cluster);
        when(cluster.health(any(HealthRequest.class))).thenReturn(CompletableFuture.completedFuture(response));
        when(response.clusterName()).thenReturn("test-cluster");
        when(response.status()).thenReturn(HealthStatus.Yellow);
        when(response.numberOfNodes()).thenReturn(1);
        when(response.numberOfDataNodes()).thenReturn(1);
        when(response.timedOut()).thenReturn(true);
        var result = service.health();
        verifyNoInteractions(client, cluster);
        StepVerifier.create(result).assertNext(health -> {
            assertThat(health.clusterName()).isEqualTo("test-cluster");
            assertThat(health.status()).isEqualTo("yellow");
            assertThat(health.numberOfNodes()).isEqualTo(1);
            assertThat(health.numberOfDataNodes()).isEqualTo(1);
            assertThat(health.timedOut()).isTrue();
        }).verifyComplete();
    }

    private static InfoResponse infoResponse() {
        var response = mock(InfoResponse.class);
        var version = mock(OpenSearchVersionInfo.class);
        when(response.clusterName()).thenReturn("test-cluster");
        when(response.clusterUuid()).thenReturn("test-uuid");
        when(response.name()).thenReturn("test-node");
        when(response.version()).thenReturn(version);
        when(version.distribution()).thenReturn("opensearch");
        when(version.number()).thenReturn("3.9.0");
        return response;
    }
}
