package com.example.learning.opensearch.securitytls.support;

import com.example.learning.opensearch.securitytls.config.OpenSearchProperties;
import com.example.learning.opensearch.securitytls.config.OpenSearchTlsContextFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Comparator;

public final class SecureOpenSearchTestEnvironment implements AutoCloseable {

    private final Path material;
    private final Network network = Network.newNetwork();
    private final GenericContainer<?> node;

    public SecureOpenSearchTestEnvironment(boolean hostnameMismatch) throws Exception {
        material = Path.of("target", "security-it-" + java.util.UUID.randomUUID()).toAbsolutePath();
        var preparation = new ProcessBuilder("bash", "scripts/prepare-security.sh", material.toString())
                .redirectErrorStream(true).redirectOutput(material.resolveSibling(material.getFileName() + ".log").toFile()).start();
        if (!preparation.waitFor(120, java.util.concurrent.TimeUnit.SECONDS)) {
            preparation.descendants().forEach(ProcessHandle::destroyForcibly);
            preparation.destroyForcibly().waitFor();
            deleteMaterial();
            throw new IllegalStateException("Security fixture preparation timed out");
        }
        if (preparation.exitValue() != 0) {
            deleteMaterial();
            throw new IllegalStateException("Security fixture preparation failed; see target security-it log");
        }
        node = new GenericContainer<>(DockerImageName.parse(System.getProperty("opensearch.image")))
                .withNetwork(network).withNetworkAliases("opensearch")
                .withCreateContainerCmdModifier(command -> command.withHostName("opensearch"))
                .withExposedPorts(9200)
                .withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
                .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
                .withCopyFileToContainer(MountableFile.forHostPath(Path.of("config/opensearch.yml").toAbsolutePath(), 0644),
                        "/usr/share/opensearch/config/opensearch.yml")
                .waitingFor(Wait.forLogMessage(".*Node.*started.*\n", 1).withStartupTimeout(Duration.ofMinutes(3)));
        copy(node, "public/ca.pem", "/usr/share/opensearch/config/certs/public/ca.pem");
        copy(node, "node/node.pem", "/usr/share/opensearch/config/certs/node/node.pem");
        copy(node, "node/node.key", "/usr/share/opensearch/config/certs/node/node.key");
        copy(node, hostnameMismatch ? "http/mismatch.pem" : "http/http.pem", "/usr/share/opensearch/config/certs/http/http.pem");
        copy(node, "http/http.key", "/usr/share/opensearch/config/certs/http/http.key");
        try {
            node.start();
            if (!hostnameMismatch) {
                initialize();
                awaitAuthenticatedReadiness();
            }
        } catch (Exception exception) {
            System.err.println(node.getLogs());
            close();
            throw exception;
        }
    }

    private void copy(GenericContainer<?> container, String source, String target) {
        container.withCopyFileToContainer(MountableFile.forHostPath(material.resolve(source), 0644), target);
    }

    private void initialize() throws IOException {
        try (var bootstrap = new GenericContainer<>(DockerImageName.parse(System.getProperty("opensearch.image")))
                .withNetwork(network)
                .withCreateContainerCmdModifier(command -> command.withEntrypoint("bash"))
                .withCommand("-ec", "plugins/opensearch-security/tools/securityadmin.sh -h opensearch -p 9200 -cn learning-ch02 "
                        + "-cd /bootstrap/security -cacert /bootstrap/public/ca.pem -cert /bootstrap/admin/admin.pem -key /bootstrap/admin/admin.key")
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofMinutes(2)))) {
            copy(bootstrap, "public/ca.pem", "/bootstrap/public/ca.pem");
            copy(bootstrap, "admin/admin.pem", "/bootstrap/admin/admin.pem");
            copy(bootstrap, "admin/admin.key", "/bootstrap/admin/admin.key");
            try (var files = Files.list(material.resolve("security"))) {
                files.forEach(file -> copy(bootstrap, "security/" + file.getFileName(), "/bootstrap/security/" + file.getFileName()));
            }
            bootstrap.start();
            if (!bootstrap.getLogs().contains("Done with success")) {
                throw new IllegalStateException("Security initialization failed: " + bootstrap.getLogs());
            }
        }
    }

    private void awaitAuthenticatedReadiness() throws Exception {
        for (int attempt = 0; attempt < 30; attempt++) {
            if (raw("monitor", "GET", "/_cluster/health?wait_for_status=yellow&timeout=2s").statusCode() == 200) {
                return;
            }
            Thread.sleep(200);
        }
        throw new IllegalStateException("Authenticated OpenSearch readiness did not pass");
    }

    public HttpResponse<String> raw(String identity, String method, String path) throws Exception {
        try (var client = HttpClient.newBuilder().sslContext(OpenSearchTlsContextFactory.create(material.resolve("public/ca.pem")))
                .connectTimeout(Duration.ofSeconds(2)).build()) {
            var user = identity.equals("info") ? "ch02_info_only" : "ch02_monitor";
            var password = Files.readString(material.resolve("secrets/" + identity + ".password")).strip();
            var authorization = Base64.getEncoder().encodeToString((user + ":" + password).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var request = HttpRequest.newBuilder(uri().resolve(path)).timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Basic " + authorization).method(method, HttpRequest.BodyPublishers.noBody()).build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    public URI uri() {
        return URI.create("https://" + node.getHost() + ":" + node.getMappedPort(9200));
    }

    public Path material() {
        return material;
    }

    public OpenSearchProperties properties(String identity, boolean wrongCa) {
        return new OpenSearchProperties(uri(), material.resolve(wrongCa ? "public/unrelated-ca.pem" : "public/ca.pem"),
                identity.equals("info") ? "ch02_info_only" : "ch02_monitor", material.resolve("secrets/" + identity + ".password"),
                Duration.ofSeconds(2), Duration.ofSeconds(2), Duration.ofSeconds(5), Duration.ofSeconds(10));
    }

    public static boolean hasTlsCause(Throwable failure) {
        for (int depth = 0; failure != null && depth < 16; depth++, failure = failure.getCause()) {
            if (failure instanceof SSLException) {
                return true;
            }
        }
        return false;
    }

    private void deleteMaterial() throws IOException {
        if (Files.exists(material)) {
            try (var files = Files.walk(material)) {
                for (var file : files.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(file);
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        try {
            node.close();
        } finally {
            network.close();
            deleteMaterial();
        }
    }
}
