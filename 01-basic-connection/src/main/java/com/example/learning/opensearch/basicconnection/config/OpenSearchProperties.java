package com.example.learning.opensearch.basicconnection.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties("learning.opensearch")
public record OpenSearchProperties(
    @NotNull URI uri,
    @NotNull Duration connectTimeout,
    @NotNull Duration poolTimeout,
    @NotNull Duration responseTimeout,
    @NotNull Duration requestTimeout
) {

    @AssertTrue(message = "Chapter 01 requires an http URI with host, no credentials, path, query or fragment")
    public boolean isEndpointValid() {
        return uri != null && "http".equals(uri.getScheme()) && uri.getHost() != null
                && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null
                && (uri.getPath().isEmpty() || "/".equals(uri.getPath()))
                && (uri.getPort() == -1 || (uri.getPort() > 0 && uri.getPort() <= 65535));
    }

    @AssertTrue(message = "Timeouts must be between 1ms and 60s")
    public boolean isTimeoutsValid() {
        return validTimeout(connectTimeout) && validTimeout(poolTimeout)
                && validTimeout(responseTimeout) && validTimeout(requestTimeout);
    }

    private static boolean validTimeout(Duration value) {
        return value != null && value.compareTo(Duration.ofMillis(1)) >= 0
                && value.compareTo(Duration.ofSeconds(60)) <= 0;
    }
}
