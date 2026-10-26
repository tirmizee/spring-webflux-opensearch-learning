package com.example.learning.opensearch.apikey.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.ResponseException;
import org.opensearch.client.Response;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.message.StatusLine;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @ParameterizedTest
    @CsvSource({"401,OPENSEARCH_AUTHENTICATION_FAILED", "403,OPENSEARCH_AUTHORIZATION_FAILED"})
    void handlesJsonAndPlainTextSecurityResponses(int status, String code) {
        var json = new OpenSearchException(new ErrorResponse.Builder().status(status)
                .error(error -> error.type("security_exception").reason("private-password")).build());
        var plain = mock(ResponseException.class);
        var response = mock(Response.class);
        when(response.getStatusLine()).thenReturn(new StatusLine(HttpVersion.HTTP_1_1, status, "test-only"));
        when(plain.getResponse()).thenReturn(response);
        for (var failure : new Exception[]{json, plain}) {
            var result = handler.upstream(new IOException("private-host", failure));
            assertThat(result.getStatus()).isEqualTo(502);
            assertThat(result.getProperties()).containsEntry("code", code);
            assertThat(result.toString()).doesNotContain("private-password", "private-host");
        }
    }

    @Test
    void tlsCauseWinsOverIoWrapperAndIsSanitized() {
        var result = handler.upstream(new IOException("private-cert-path", new SSLHandshakeException("private-DN")));
        assertThat(result.getStatus()).isEqualTo(502);
        assertThat(result.getProperties()).containsEntry("code", "OPENSEARCH_TLS_FAILURE");
        assertThat(result.toString()).doesNotContain("private-cert-path", "private-DN");
    }

    @Test
    void distinguishesDeadlineSocketTimeoutAndUnavailable() {
        for (var failure : new Exception[]{new TimeoutException(), new SocketTimeoutException()}) {
            var result = handler.upstream(failure);
            assertThat(result.getStatus()).isEqualTo(504);
            assertThat(result.getProperties()).containsEntry("code", "OPENSEARCH_TIMEOUT");
        }
        var result = handler.upstream(new IOException("private"));
        assertThat(result.getStatus()).isEqualTo(503);
        assertThat(result.getProperties()).containsEntry("code", "OPENSEARCH_UNAVAILABLE");
    }
}
