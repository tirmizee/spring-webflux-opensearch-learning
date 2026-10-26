package com.example.learning.opensearch.apikey.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.opensearch.data.client.osc.ReactiveOpenSearchTemplate;
import org.opensearch.spring.boot.autoconfigure.OpenSearchClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.ReactiveOpenSearchClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.RestClientBuilderCustomizer;
import org.opensearch.spring.boot.autoconfigure.data.OpenSearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationTest {

    @TempDir
    private Path temporary;

    private ApplicationContextRunner runner() throws Exception {
        Files.writeString(temporary.resolve("token"), "os_test-only\n");
        return new ApplicationContextRunner()
                .withUserConfiguration(ApiKeyConfiguration.class)
                .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class,
                        OpenSearchRestClientAutoConfiguration.class, OpenSearchClientAutoConfiguration.class,
                        ReactiveOpenSearchClientAutoConfiguration.class, OpenSearchDataAutoConfiguration.class))
                .withPropertyValues("opensearch.api-key-file=" + temporary.resolve("token"),
                        "opensearch.connection-timeout=2s", "opensearch.request-timeout=5s",
                        "opensearch.uris=https://localhost:9203", "opensearch.socket-timeout=3s");
    }

    @Test
    void starterOwnsClientTransportTemplateAndConverter() throws Exception {
        runner().run(context -> {
            assertThat(context).hasNotFailed();
            var transport = context.getBean(OpenSearchTransport.class);
            assertThat(transport.getClass().getName())
                    .isEqualTo("org.opensearch.client.transport.rest_client.RestClientTransport");
            assertThat(context.getBean(ReactiveOpenSearchClient.class)._transport()).isSameAs(transport);
            assertThat(context.getBean(ReactiveElasticsearchOperations.class)).isInstanceOf(ReactiveOpenSearchTemplate.class);
            assertThat(context.getBean(ElasticsearchConverter.class)).isNotNull();
            assertThat(context.getBeanFactory().getBeanDefinition("opensearchRestClient").getFactoryBeanName())
                    .contains("RestClientConfiguration");
            assertThat(context.getBeanFactory().getBeanDefinition("restClientTransport").getFactoryBeanName())
                    .contains("OpenSearchTransportConfiguration");
            assertThat(context.getBeanFactory().getBeanDefinition("reactiveOpensearchClient").getFactoryBeanName())
                    .contains("ReactiveOpenSearchClientAutoConfiguration");
            assertThat(context.getBeanFactory().getBeanDefinition("reactiveElasticsearchTemplate").getFactoryBeanName())
                    .contains("OpenSearchDataConfiguration");
            assertThat(context.getBeanFactory().getBeanDefinition("elasticsearchConverter").getFactoryBeanName())
                    .contains("OpenSearchDataConfiguration");
            var request = RequestConfig.custom();
            context.getBean("apiKeyCustomizer", RestClientBuilderCustomizer.class).customize(request);
            assertThat(request.build().getResponseTimeout()).isEqualTo(Timeout.ofSeconds(3));
        });
    }

    @Test
    void missingKeyFailsStartup() throws Exception {
        runner().withPropertyValues("opensearch.api-key-file=" + temporary.resolve("missing"))
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"opensearch.uris=http://localhost:9203",
            "opensearch.uris=https://user:pass@localhost:9203", "opensearch.uris=https://localhost/path",
            "opensearch.uris=https://localhost:0", "opensearch.uris=https://localhost?q=1",
            "opensearch.uris=https://localhost#fragment", "opensearch.uris=https://localhost/", "opensearch.uris=https://localhost:9203,https://localhost:9204",
            "opensearch.username=test-user", "opensearch.password=test-only", "opensearch.path-prefix=/test",
            "opensearch.socket-timeout=10s", "opensearch.connection-timeout=0s",
            "opensearch.connection-timeout=61s", "opensearch.socket-timeout=0s",
            "opensearch.request-timeout=0s", "opensearch.request-timeout=61s", "opensearch.request-timeout=1s"})
    void rejectsUnsafeOrInconsistentInputs(String property) throws Exception {
        runner().withPropertyValues(property).run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://localhost:9203", "https://127.0.0.1:9203", "https://localhost"})
    void acceptsSafeHttpsUris(String uri) throws Exception {
        runner().withPropertyValues("opensearch.uris=" + uri).run(context -> assertThat(context).hasNotFailed());
    }
}
