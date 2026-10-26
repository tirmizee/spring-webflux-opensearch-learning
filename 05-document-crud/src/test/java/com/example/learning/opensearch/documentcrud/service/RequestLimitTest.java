package com.example.learning.opensearch.documentcrud.service;

import com.example.learning.opensearch.documentcrud.exception.ChapterException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLimitTest {

    @Test
    void rejectsSeventeenthWithoutCallingItAndReleasesOnCancel() {
        var limit = new RequestLimit();
        var invoked = new AtomicBoolean();
        // Nested subscriptions via StepVerifier hold all 16 permits without manual subscribe.
        StepVerifier.create(reactor.core.publisher.Flux.range(0, 16)
                .flatMap(i -> limit.within(() -> Mono.never()), 16))
                .then(() -> StepVerifier.create(limit.within(() -> {
                    invoked.set(true);
                    return Mono.just(1);
                })).expectErrorSatisfies(error ->
                        assertThat(((ChapterException) error).code()).isEqualTo("REQUEST_LIMIT_EXCEEDED")).verify())
                .thenCancel().verify();
        assertThat(invoked).isFalse();
        StepVerifier.create(limit.within(() -> Mono.just(1))).expectNext(1).verifyComplete();
    }

    @Test
    void releasesAfterSyncThrowErrorAndSuccessWithoutLeakingPermits() {
        var limit = new RequestLimit();
        for (int i = 0; i < 20; i++) {
            StepVerifier.create(limit.within(() -> { throw new IllegalStateException("sync"); }))
                    .expectErrorMessage("sync").verify();
            StepVerifier.create(limit.within(() -> Mono.error(new IllegalArgumentException("async"))))
                    .expectErrorMessage("async").verify();
            StepVerifier.create(limit.within(() -> Mono.just(42))).expectNext(42).verifyComplete();
        }
    }

}
