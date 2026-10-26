package com.example.learning.opensearch.mappings.exception;

import org.opensearch.client.ResponseException;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ChapterException.class)
    public ProblemDetail chapter(ChapterException exception) {
        return problem(exception.status(), exception.code());
    }

    @ExceptionHandler({ServerWebInputException.class, ResponseStatusException.class})
    public ProblemDetail input(ResponseStatusException exception) {
        return problem(HttpStatus.valueOf(exception.getStatusCode().value()), "INVALID_REQUEST");
    }

    @ExceptionHandler({DataAccessException.class, IOException.class, OpenSearchException.class, TimeoutException.class})
    public ProblemDetail upstream(Exception exception) {
        var seen = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        Throwable cause = exception;
        Integer status = null;
        boolean timeout = false;
        boolean missing = false;
        boolean exists = false;
        for (int depth = 0; cause != null && depth < 16 && seen.add(cause); depth++, cause = cause.getCause()) {
            timeout |= cause instanceof TimeoutException || cause instanceof SocketTimeoutException;
            if (cause instanceof OpenSearchException search) {
                status = search.status();
                exists |= "resource_already_exists_exception".equals(search.error().type());
                missing |= "index_not_found_exception".equals(search.error().type());
            } else if (cause instanceof ResponseException response) {
                status = response.getResponse().getStatusLine().getStatusCode();
            }
        }
        if (exists) {
            return problem(HttpStatus.CONFLICT, "INDEX_ALREADY_EXISTS");
        }
        if (missing || Integer.valueOf(404).equals(status)) {
            return problem(HttpStatus.NOT_FOUND, "INDEX_NOT_FOUND");
        }
        if (timeout) {
            return problem(HttpStatus.GATEWAY_TIMEOUT, "OPENSEARCH_TIMEOUT");
        }
        if (Integer.valueOf(400).equals(status)) {
            return problem(HttpStatus.BAD_REQUEST, "OPENSEARCH_REQUEST_REJECTED");
        }
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "OPENSEARCH_UNAVAILABLE");
    }

    private static ProblemDetail problem(HttpStatus status, String code) {
        var body = ProblemDetail.forStatusAndDetail(status, "The chapter operation could not be completed.");
        body.setTitle("Chapter 04 operation failed");
        body.setProperty("code", code);
        return body;
    }
}
