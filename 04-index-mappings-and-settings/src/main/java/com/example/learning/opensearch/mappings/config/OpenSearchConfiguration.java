package com.example.learning.opensearch.mappings.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.core5.util.Timeout;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.spring.boot.autoconfigure.OpenSearchProperties;
import org.opensearch.spring.boot.autoconfigure.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class OpenSearchConfiguration {

    @Bean
    RestClientBuilderCustomizer responseTimeoutCustomizer(OpenSearchProperties properties, LearningProperties learning) {
        if (properties.getUris() == null || properties.getUris().size() != 1 || properties.getUsername() != null || properties.getPassword() != null
                || properties.getPathPrefix() != null) {
            throw new IllegalArgumentException("Chapter 04 requires one HTTP URI without credentials or path prefix");
        }
        var uri = URI.create(properties.getUris().getFirst());
        if (!"http".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                || !uri.getPath().isEmpty() || uri.getQuery() != null || uri.getFragment() != null
                || (uri.getPort() != -1 && (uri.getPort() <= 0 || uri.getPort() > 65535))) {
            throw new IllegalArgumentException("Chapter 04 requires an HTTP host URI without path, query or fragment");
        }
        if (!valid(properties.getConnectionTimeout()) || !valid(properties.getSocketTimeout())
                || learning.requestTimeout().compareTo(properties.getSocketTimeout()) < 0) {
            throw new IllegalArgumentException("Transport timeouts must be 1ms-60s and request-timeout >= socket-timeout");
        }
        return new RestClientBuilderCustomizer() {
            @Override
            public void customize(RestClientBuilder builder) {
                // Starter owns the builder/client; no authentication headers or TLS configuration.
            }

            @Override
            public void customize(HttpAsyncClientBuilder builder) {
                builder.disableAutomaticRetries().disableRedirectHandling();
            }

            @Override
            public void customize(RequestConfig.Builder builder) {
                // Starter 3.1.4 does not apply socket-timeout to response timeout itself.
                builder.setResponseTimeout(Timeout.ofMilliseconds(properties.getSocketTimeout().toMillis()));
            }
        };
    }

    private static boolean valid(Duration duration) {
        return duration != null && duration.compareTo(Duration.ofMillis(1)) >= 0
                && duration.compareTo(Duration.ofSeconds(60)) <= 0;
    }
}
