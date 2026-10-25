package com.example.learning.opensearch.basicconnection.exception;

import org.opensearch.client.opensearch._types.OpenSearchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({TimeoutException.class, SocketTimeoutException.class})
    ProblemDetail timeout(Exception exception) {
        return problem(HttpStatus.GATEWAY_TIMEOUT, "OpenSearch timeout", "OpenSearch did not respond within the configured deadline.");
    }

    @ExceptionHandler({IOException.class, OpenSearchException.class})
    ProblemDetail unavailable(Exception exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "OpenSearch unavailable", "The OpenSearch dependency could not serve this request.");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
