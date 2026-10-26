package com.example.learning.opensearch.documentcrud.service;

import com.example.learning.opensearch.documentcrud.exception.ChapterException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

@Component
public class RequestLimit {

    private final Semaphore permits = new Semaphore(16);

    public <T> Mono<T> within(Supplier<Mono<T>> operation) {
        // No waiting queue; using releases the permit on completion, error, synchronous throw or cancellation.
        return Mono.using(() -> {
            if (!permits.tryAcquire()) {
                throw new ChapterException(HttpStatus.SERVICE_UNAVAILABLE, "REQUEST_LIMIT_EXCEEDED");
            }
            return permits;
        }, ignored -> Mono.defer(operation), Semaphore::release);
    }
}
