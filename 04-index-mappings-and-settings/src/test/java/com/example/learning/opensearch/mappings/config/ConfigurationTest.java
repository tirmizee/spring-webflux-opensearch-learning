package com.example.learning.opensearch.mappings.config;

import com.example.learning.opensearch.mappings.MappingsApplication;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.opensearch.data.client.osc.ReactiveOpenSearchTemplate;
import org.opensearch.spring.boot.autoconfigure.RestClientBuilderCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MappingsApplication.class)
            .withPropertyValues("learning.index-name=ch04-unit", "learning.request-timeout=5s",
                    "opensearch.uris=http://localhost:9204", "opensearch.connection-timeout=1s",
                    "opensearch.socket-timeout=3s");

    @Test
    void starterOwnsInfrastructureAndResponseTimeoutIsApplied() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            var transport = context.getBean(OpenSearchTransport.class);
            assertThat(transport.getClass().getName())
                    .isEqualTo("org.opensearch.client.transport.rest_client.RestClientTransport");
            assertThat(context.getBean(ReactiveOpenSearchClient.class)._transport()).isSameAs(transport);
            assertThat(context.getBean(ReactiveElasticsearchOperations.class)).isInstanceOf(ReactiveOpenSearchTemplate.class);
            assertThat(context.getBean(ElasticsearchConverter.class)).isNotNull();
            assertThat(context.getBeanFactory().getBeanDefinition("restClientTransport").getFactoryBeanName())
                    .contains("OpenSearchTransportConfiguration");
            assertThat(context.getBeanFactory().getBeanDefinition("reactiveOpensearchClient").getFactoryBeanName())
                    .contains("ReactiveOpenSearchClientAutoConfiguration");
            assertThat(context.getBeanFactory().getBeanDefinition("reactiveElasticsearchTemplate").getFactoryBeanName())
                    .contains("OpenSearchDataConfiguration");
            var request = RequestConfig.custom();
            context.getBean("responseTimeoutCustomizer", RestClientBuilderCustomizer.class).customize(request);
            assertThat(request.build().getResponseTimeout()).isEqualTo(Timeout.ofSeconds(3));
            assertThat(context.containsBean("opensearchRestHighLevelClient")).isFalse();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"opensearch.uris=https://localhost:9204", "opensearch.uris=http://user:pass@localhost:9204",
            "opensearch.uris=http://localhost/path", "opensearch.uris=http://localhost:0",
            "opensearch.uris=http://localhost:9204,http://localhost:9205", "opensearch.username=test-only",
            "opensearch.password=test-only", "opensearch.path-prefix=/test", "opensearch.socket-timeout=0s",
            "opensearch.connection-timeout=0s", "opensearch.connection-timeout=61s", "opensearch.socket-timeout=6s",
            "learning.request-timeout=0s", "learning.request-timeout=61s", "learning.index-name=other-index",
            "learning.index-name=ch04-*", "learning.index-name=ch04-UPPER"})
    void rejectsOutOfScopeOrInconsistentConfiguration(String property) {
        runner.withPropertyValues(property).run(context -> assertThat(context).hasFailed());
    }
}
