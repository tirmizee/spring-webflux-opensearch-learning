package com.example.learning.opensearch.securitytls.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

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
    @NotNull Duration requestTimeout
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

    private static boolean valid(Duration value) {
        return value != null && value.compareTo(Duration.ofMillis(1)) >= 0
                && value.compareTo(Duration.ofSeconds(60)) <= 0;
    }
}
