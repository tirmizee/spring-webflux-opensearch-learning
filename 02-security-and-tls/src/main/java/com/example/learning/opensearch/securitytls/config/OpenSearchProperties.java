package com.example.learning.opensearch.securitytls.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

@Validated
@ConfigurationProperties("learning.opensearch")
public record OpenSearchProperties(
    @NotNull URI uri,
    @NotNull Path caCertificate,
    @NotBlank String username,
    @NotNull Path passwordFile,
    @NotNull Duration connectTimeout,
    @NotNull Duration poolTimeout,
    @NotNull Duration responseTimeout,
    @NotNull Duration requestTimeout,
    @DefaultValue("16") @Min(1) int maxConnections,
    @DefaultValue("16") @Min(1) int maxConnectionsPerRoute
) {

    @AssertTrue(message = "HTTPS URI requires a host and no credentials, path, query or fragment")
    public boolean isEndpointValid() {
        return uri != null && "https".equals(uri.getScheme()) && uri.getHost() != null
                && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null
                && (uri.getPath().isEmpty() || "/".equals(uri.getPath()))
                && (uri.getPort() == -1 || (uri.getPort() > 0 && uri.getPort() <= 65535));
    }

    @AssertTrue(message = "Timeouts must be between 1ms and 60s")
    public boolean isTimeoutsValid() {
        return valid(connectTimeout) && valid(poolTimeout) && valid(responseTimeout) && valid(requestTimeout);
    }

    @AssertTrue(message = "Request timeout must be greater than or equal to response timeout")
    public boolean isTimeoutOrderValid() {
        return requestTimeout != null && responseTimeout != null
                && requestTimeout.compareTo(responseTimeout) >= 0;
    }

    @AssertTrue(message = "Max connections per route must not exceed max connections")
    public boolean isConnectionPoolValid() {
        return maxConnectionsPerRoute <= maxConnections;
    }

    private static boolean valid(Duration value) {
        return value != null && value.compareTo(Duration.ofMillis(1)) >= 0
                && value.compareTo(Duration.ofSeconds(60)) <= 0;
    }
}
