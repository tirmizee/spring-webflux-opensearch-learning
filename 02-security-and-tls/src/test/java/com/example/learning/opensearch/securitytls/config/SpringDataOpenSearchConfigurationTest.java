package com.example.learning.opensearch.securitytls.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.opensearch.data.client.osc.ReactiveOpenSearchTemplate;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SpringDataOpenSearchConfigurationTest {

    @TempDir
    private Path temporary;
    private ApplicationContextRunner runner;

    @BeforeEach
    void prepareCertificateAndProperties() throws Exception {
        Files.copy(Path.of(SpringDataOpenSearchConfigurationTest.class.getResource("/certs/unit-ca.pem").toURI()), temporary.resolve("ca.pem"));
        Files.writeString(temporary.resolve("password"), " test-password \n");
        runner = new ApplicationContextRunner()
                .withUserConfiguration(SpringDataOpenSearchConfiguration.class)
                .withBean(JsonMapper.class, () -> JsonMapper.builder().build())
                .withPropertyValues("learning.opensearch.uri=https://localhost:9202",
                        "learning.opensearch.ca-certificate=" + temporary.resolve("ca.pem"),
                        "learning.opensearch.username=ch02_monitor",
                        "learning.opensearch.password-file=" + temporary.resolve("password"),
                        "learning.opensearch.connect-timeout=1s", "learning.opensearch.pool-timeout=1s",
                        "learning.opensearch.response-timeout=3s", "learning.opensearch.request-timeout=5s");
    }

    @Test
    void frameworkProvidesClientTemplateConverterAndSeparateTransport() {
        runner.withUserConfiguration(OpenSearchConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            var settings = context.getBean(OpenSearchProperties.class);
            assertThat(settings.maxConnections()).isEqualTo(16);
            assertThat(settings.maxConnectionsPerRoute()).isEqualTo(16);
            var springTransport = context.getBean("springDataTransport", OpenSearchTransport.class);
            var javaTransport = context.getBean("javaClientTransport", OpenSearchTransport.class);
            assertThat(springTransport).isInstanceOf(ApacheHttpClient5Transport.class).isNotSameAs(javaTransport);
            assertThat(javaTransport).isInstanceOf(ApacheHttpClient5Transport.class);
            assertThat(context.getBean(ReactiveOpenSearchClient.class)._transport()).isSameAs(springTransport);
            assertThat(context.getBean(OpenSearchAsyncClient.class)._transport()).isSameAs(javaTransport);
            assertThat(context.getBean(ReactiveElasticsearchOperations.class)).isInstanceOf(ReactiveOpenSearchTemplate.class);
            assertThat(context.getBean(ElasticsearchConverter.class)).isNotNull();
            assertThat(context.getBeansOfType(OpenSearchTransport.class)).hasSize(2);
        });
    }

    @Test
    void onlySeparateTransportsOwnShutdown() {
        runner.withUserConfiguration(OpenSearchConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean("elasticsearchRestClient");
            for (String owner : new String[]{"javaClientTransport", "springDataTransport"}) {
                var definition = (AbstractBeanDefinition) context.getBeanFactory().getBeanDefinition(owner);
                assertThat(definition.getDestroyMethodNames()).containsExactly("close");
            }
            for (String wrapper : new String[]{"reactiveOpenSearchClient"}) {
                var definition = (AbstractBeanDefinition) context.getBeanFactory().getBeanDefinition(wrapper);
                assertThat(definition.getDestroyMethodNames()).containsExactly("");
            }
        });
    }

    @Test
    void bindsCustomPoolLimits() {
        runner.withPropertyValues("learning.opensearch.max-connections=8",
                "learning.opensearch.max-connections-per-route=2").run(context -> {
                    assertThat(context).hasNotFailed();
                    var settings = context.getBean(OpenSearchProperties.class);
                    assertThat(settings.maxConnections()).isEqualTo(8);
                    assertThat(settings.maxConnectionsPerRoute()).isEqualTo(2);
                });
    }

    @Test
    void rejectsInvalidPoolAtStartup() {
        runner.withPropertyValues("learning.opensearch.max-connections=2",
                "learning.opensearch.max-connections-per-route=3")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsRequestDeadlineShorterThanResponseTimeoutAtStartup() {
        runner.withPropertyValues("learning.opensearch.request-timeout=2s")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void missingPasswordFailsStartupInsteadOfCreatingAnonymousClient() {
        runner.withPropertyValues("learning.opensearch.password-file=" + temporary.resolve("missing"))
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .hasRootCauseInstanceOf(java.nio.file.NoSuchFileException.class));
    }
}
