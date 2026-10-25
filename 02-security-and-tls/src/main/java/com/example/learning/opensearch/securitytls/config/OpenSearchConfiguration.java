package com.example.learning.opensearch.securitytls.config;

import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchConfiguration {

    @Bean(name = "javaClientTransport", destroyMethod = "close")
    OpenSearchTransport openSearchTransport(OpenSearchProperties properties, JsonMapper mapper)
            throws IOException, GeneralSecurityException {
        return OpenSearchTransportFactory.create(properties, mapper);
    }

    @Bean
    OpenSearchAsyncClient openSearchAsyncClient(@Qualifier("javaClientTransport") OpenSearchTransport transport) {
        return new OpenSearchAsyncClient(transport);
    }
}
