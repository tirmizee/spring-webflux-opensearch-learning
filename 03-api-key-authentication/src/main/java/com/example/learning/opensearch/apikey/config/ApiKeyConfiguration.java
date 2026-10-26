package com.example.learning.opensearch.apikey.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.util.Timeout;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.spring.boot.autoconfigure.OpenSearchProperties;
import org.opensearch.spring.boot.autoconfigure.RestClientBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ApiKeyConnectionProperties.class)
public class ApiKeyConfiguration {

    @Bean
    RestClientBuilderCustomizer apiKeyCustomizer(ApiKeyConnectionProperties properties, OpenSearchProperties defaults) throws IOException {

        validate(defaults, properties.requestTimeout());

        // Read once during startup, outside the Reactor request pipeline.
        var token = OpenSearchApiKeyFile.read(properties.apiKeyFile());

        return new RestClientBuilderCustomizer() {
            @Override
            public void customize(RestClientBuilder builder) {
                builder.setDefaultHeaders(new Header[]{new BasicHeader("Authorization", "ApiKey " + token)});
            }

            @Override
            public void customize(HttpAsyncClientBuilder builder) {
                builder.disableRedirectHandling().disableAutomaticRetries();
            }

            @Override
            public void customize(RequestConfig.Builder builder) {
                // Starter 3.1.4 does not apply socket-timeout to response timeout itself.
                builder.setResponseTimeout(Timeout.ofMilliseconds(defaults.getSocketTimeout().toMillis()));
            }
        };
    }

    private static void validate(OpenSearchProperties properties, Duration requestTimeout) {
        if (properties.getUris() == null || properties.getUris().size() != 1
                || properties.getUsername() != null || properties.getPassword() != null
                || properties.getPathPrefix() != null) {
            throw new IllegalArgumentException("Use one HTTPS URI and api-key-file without Basic credentials or path prefix");
        }
        var uri = URI.create(properties.getUris().getFirst());
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null
                || !uri.getPath().isEmpty()
                || (uri.getPort() != -1 && (uri.getPort() <= 0 || uri.getPort() > 65535))) {
            throw new IllegalArgumentException("HTTPS URI requires a host and no credentials, path, query or fragment");
        }
        if (!valid(properties.getConnectionTimeout()) || !valid(properties.getSocketTimeout())
                || requestTimeout.compareTo(properties.getSocketTimeout()) < 0) {
            throw new IllegalArgumentException("Timeouts must be between 1ms and 60s; request-timeout must be >= socket-timeout");
        }
    }

    private static boolean valid(Duration duration) {
        return duration != null && duration.compareTo(Duration.ofMillis(1)) >= 0
                && duration.compareTo(Duration.ofSeconds(60)) <= 0;
    }
}
