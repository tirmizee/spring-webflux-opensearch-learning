package com.example.learning.opensearch.basicconnection.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.opensearch.client.json.jackson3.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchConfiguration {

    @Bean(destroyMethod = "close")
    OpenSearchTransport openSearchTransport(OpenSearchProperties properties, JsonMapper mapper) {
        var connections = PoolingAsyncClientConnectionManagerBuilder.create()
                .setMaxConnTotal(16)
                .setMaxConnPerRoute(16)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(properties.connectTimeout().toMillis()))
                        .build())
                .build();
        return ApacheHttpClient5TransportBuilder.builder(HttpHost.create(properties.uri()))
                .setMapper(new JacksonJsonpMapper(mapper))
                .setAutomaticRetriesDisabled(true)
                .setHttpClientConfigCallback(client -> client.setConnectionManager(connections))
                .setRequestConfigCallback(request -> request
                        .setConnectionRequestTimeout(Timeout.ofMilliseconds(properties.poolTimeout().toMillis()))
                        .setResponseTimeout(Timeout.ofMilliseconds(properties.responseTimeout().toMillis())))
                .build();
    }

    // transport bean เป็น close owner เดียว; ไม่ใช้ lifecycle callback ของ reactive wrapper
    @Bean(destroyMethod = "")
    ReactiveOpenSearchClient reactiveOpenSearchClient(OpenSearchTransport transport) {
        return new ReactiveOpenSearchClient(transport);
    }

    @Bean
    OpenSearchAsyncClient openSearchAsyncClient(OpenSearchTransport transport) {
        return new OpenSearchAsyncClient(transport);
    }
}
