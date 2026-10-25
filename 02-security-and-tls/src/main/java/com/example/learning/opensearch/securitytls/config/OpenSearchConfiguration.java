package com.example.learning.opensearch.securitytls.config;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.opensearch.client.json.jackson3.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchConfiguration {

    @Bean(destroyMethod = "close")
    OpenSearchTransport openSearchTransport(OpenSearchProperties properties, JsonMapper mapper)
            throws IOException, GeneralSecurityException {
        // File I/O เกิดเฉพาะ startup ไม่อยู่ใน Reactor request pipeline
        var sslContext = OpenSearchTlsContextFactory.create(properties.caCertificate());
        var password = readPassword(properties.passwordFile());
        var host = HttpHost.create(properties.uri());
        var credentials = new BasicCredentialsProvider();
        credentials.setCredentials(new AuthScope(host),
                new UsernamePasswordCredentials(properties.username(), password.toCharArray()));
        var tls = ClientTlsStrategyBuilder.create()
                .setSslContext(sslContext)
                .setTlsVersions("TLSv1.3", "TLSv1.2")
                .setHostVerificationPolicy(HostnameVerificationPolicy.BOTH)
                .setHostnameVerifier(new DefaultHostnameVerifier())
                .buildAsync();
        var connections = PoolingAsyncClientConnectionManagerBuilder.create()
                .setTlsStrategy(tls)
                .setMaxConnTotal(16)
                .setMaxConnPerRoute(16)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(properties.connectTimeout().toMillis()))
                        .build())
                .build();
        try {
            return ApacheHttpClient5TransportBuilder.builder(host)
                    .setMapper(new JacksonJsonpMapper(mapper))
                    .setAutomaticRetriesDisabled(true)
                    .setHttpClientConfigCallback(client -> client.setConnectionManager(connections)
                            .setDefaultCredentialsProvider(credentials).disableRedirectHandling())
                    .setRequestConfigCallback(request -> request
                            .setConnectionRequestTimeout(Timeout.ofMilliseconds(properties.poolTimeout().toMillis()))
                            .setResponseTimeout(Timeout.ofMilliseconds(properties.responseTimeout().toMillis())))
                    .build();
        } catch (RuntimeException exception) {
            connections.close();
            throw exception;
        }
    }

    static String readPassword(java.nio.file.Path path) throws IOException {
        // อนุญาต final LF/CRLF หนึ่งชุด แต่เก็บ spaces ที่เป็นส่วนหนึ่งของ password
        var password = Files.readString(path).replaceFirst("\\r?\\n\\z", "");
        if (password.isBlank() || password.contains("\n") || password.contains("\r")) {
            throw new IllegalArgumentException("Password file must contain one nonblank line");
        }
        return password;
    }

    @Bean(destroyMethod = "")
    ReactiveOpenSearchClient reactiveOpenSearchClient(OpenSearchTransport transport) {
        return new ReactiveOpenSearchClient(transport);
    }

    @Bean
    OpenSearchAsyncClient openSearchAsyncClient(OpenSearchTransport transport) {
        return new OpenSearchAsyncClient(transport);
    }
}
