package com.example.learning.opensearch.documentcrud.exception;

import org.springframework.http.HttpStatus;

public class ChapterException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ChapterException(HttpStatus status, String code) {
        super(code);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
