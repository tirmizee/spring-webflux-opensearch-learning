package com.example.learning.opensearch.basicconnection.config;

import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSearchConfigurationTest {

    @Test
    void bothClientsShareTransportAndSpringShutdownMakesItUnusable() {
        var captured = new AtomicReference<ReactiveOpenSearchClient>();
        new ApplicationContextRunner()
                .withUserConfiguration(OpenSearchConfiguration.class)
                .withBean(JsonMapper.class, () -> JsonMapper.builder().build())
                .withPropertyValues(
                        "learning.opensearch.uri=http://127.0.0.1:9200",
                        "learning.opensearch.connect-timeout=1s",
                        "learning.opensearch.pool-timeout=1s",
                        "learning.opensearch.response-timeout=3s",
                        "learning.opensearch.request-timeout=5s"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var transport = context.getBean(OpenSearchTransport.class);
                    var springData = context.getBean(ReactiveOpenSearchClient.class);
                    var direct = context.getBean(OpenSearchAsyncClient.class);
                    assertThat(springData._transport()).isSameAs(transport);
                    assertThat(direct._transport()).isSameAs(transport);
                    captured.set(springData);
                });
        // ContextRunner ปิด context แล้ว: ต้องไม่กลายเป็น connection attempt ที่รอ network timeout
        StepVerifier.create(Mono.defer(() -> captured.get().info()))
                .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(IllegalStateException.class))
                .verify(Duration.ofSeconds(2));
    }
}
