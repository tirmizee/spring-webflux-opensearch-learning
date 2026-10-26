package com.example.learning.opensearch.apikey.exception;

import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.ResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({IOException.class, OpenSearchException.class, TimeoutException.class})
    public ProblemDetail upstream(Exception exception) {

        // ตรวจ cause แบบ bounded และ cycle-safe ไม่เปิดเผย raw upstream message
        var seen = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        Throwable cause = exception;
        Integer upstreamStatus = null;
        boolean timeout = false;

        for (int depth = 0; cause != null && depth < 16 && seen.add(cause); depth++) {
            if (cause instanceof SSLException) {
                return problem(HttpStatus.BAD_GATEWAY, "OPENSEARCH_TLS_FAILURE");
            }

            if (cause instanceof TimeoutException || cause instanceof SocketTimeoutException) {
                timeout = true;
            }

            if (cause instanceof OpenSearchException search) {
                upstreamStatus = search.status();
            } else if (cause instanceof ResponseException response) {
                upstreamStatus = response.getResponse().getStatusLine().getStatusCode();
            }

            cause = cause.getCause();
        }

        if (upstreamStatus != null && upstreamStatus == 401) {
            return problem(HttpStatus.BAD_GATEWAY, "OPENSEARCH_AUTHENTICATION_FAILED");
        }

        if (upstreamStatus != null && upstreamStatus == 403) {
            return problem(HttpStatus.BAD_GATEWAY, "OPENSEARCH_AUTHORIZATION_FAILED");
        }

        return timeout
                ? problem(HttpStatus.GATEWAY_TIMEOUT, "OPENSEARCH_TIMEOUT")
                : problem(HttpStatus.SERVICE_UNAVAILABLE, "OPENSEARCH_UNAVAILABLE");
    }

    private static ProblemDetail problem(HttpStatus status, String code) {
        var problem = ProblemDetail.forStatusAndDetail(status, "The OpenSearch dependency could not serve this request.");
        problem.setTitle("OpenSearch dependency failure");
        problem.setProperty("code", code);
        return problem;
    }
}
