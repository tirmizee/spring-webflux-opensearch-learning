package com.example.learning.opensearch.documentcrud.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @ParameterizedTest
    @CsvSource({"409,version_conflict_engine_exception,409,PRODUCT_ALREADY_EXISTS",
            "404,index_not_found_exception,503,INDEX_NOT_READY", "400,mapper_parsing_exception,400,OPENSEARCH_REQUEST_REJECTED",
            "503,unavailable_shards_exception,503,OPENSEARCH_UNAVAILABLE"})
    void translatesWrappedServerErrorsWithoutLeakingDetails(int status, String type, int expected, String code) {
        var server = new OpenSearchException(new ErrorResponse.Builder().status(status)
                .error(error -> error.type(type).reason("private-server-detail")).build());
        var result = handler.upstream(new IOException("private-host", server));
        assertThat(result.getStatus()).isEqualTo(expected);
        assertThat(result.getProperties()).containsEntry("code", code);
        assertThat(result.toString()).doesNotContain("private-host", "private-server-detail");
    }

    @Test
    void nativeRestStatus409IsCreateConflictEvenWithoutParsedErrorType() {
        var response = org.mockito.Mockito.mock(org.opensearch.client.Response.class);
        var error = org.mockito.Mockito.mock(org.opensearch.client.ResponseException.class);
        org.mockito.Mockito.when(error.getResponse()).thenReturn(response);
        org.mockito.Mockito.when(response.getStatusLine()).thenReturn(new org.apache.hc.core5.http.message.StatusLine(
                new org.apache.hc.core5.http.message.BasicClassicHttpResponse(409)));
        var result = handler.upstream(error);
        assertThat(result.getStatus()).isEqualTo(409);
        assertThat(result.getProperties()).containsEntry("code", "PRODUCT_ALREADY_EXISTS");
    }

    @Test
    void timeoutsDifferFromOtherIoFailures() {
        for (var failure : new Exception[]{new TimeoutException(), new SocketTimeoutException()}) {
            var result = handler.upstream(new IOException("private-host", failure));
            assertThat(result.getStatus()).isEqualTo(504);
            assertThat(result.getProperties()).containsEntry("code", "OPENSEARCH_TIMEOUT");
        }
        assertThat(handler.upstream(new IOException()).getStatus()).isEqualTo(503);
    }
}
