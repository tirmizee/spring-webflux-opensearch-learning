package com.example.learning.opensearch.securitytls.config;

import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.opensearch.data.client.osc.ReactiveOpenSearchTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.config.ElasticsearchConfigurationSupport;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenSearchProperties.class)
public class SpringDataOpenSearchConfiguration extends ElasticsearchConfigurationSupport {

    @Bean(name = "springDataTransport", destroyMethod = "close")
    public OpenSearchTransport springDataTransport(OpenSearchProperties properties, JsonMapper mapper)
            throws IOException, GeneralSecurityException {
        // แยก instance/close owner แต่ใช้ strict TLS และ timeout policy เดียวกัน
        return OpenSearchTransportFactory.create(properties, mapper);
    }

    @Bean(destroyMethod = "")
    public ReactiveOpenSearchClient reactiveOpenSearchClient(@Qualifier("springDataTransport") OpenSearchTransport transport) {
        return new ReactiveOpenSearchClient(transport);
    }

    @Bean(name = {"reactiveElasticsearchOperations", "reactiveElasticsearchTemplate",
            "reactiveOpensearchOperations", "reactiveOpensearchTemplate"})
    public ReactiveElasticsearchOperations reactiveElasticsearchOperations(ElasticsearchConverter converter,
            ReactiveOpenSearchClient client) {
        var template = new ReactiveOpenSearchTemplate(client, converter);
        template.setRefreshPolicy(refreshPolicy());
        return template;
    }

    @Override
    protected Collection<String> getMappingBasePackages() {
        return List.of("com.example.learning.opensearch.securitytls");
    }
}
