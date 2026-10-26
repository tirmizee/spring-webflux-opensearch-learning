package com.example.learning.opensearch.apikey.support;

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
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLContext;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import tools.jackson.databind.json.JsonMapper;
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
            network.close();
            throw new IllegalStateException("Security fixture preparation timed out");
        }
        if (preparation.exitValue() != 0) {
            deleteMaterial();
            network.close();
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
                .withCommand("-ec", "plugins/opensearch-security/tools/securityadmin.sh -h opensearch -p 9200 -cn learning-ch03 "
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
            if (admin("GET", "/", null).statusCode() == 200) {
                return;
            }
            Thread.sleep(200);
        }
        throw new IllegalStateException("Administrator HTTPS readiness did not pass");
    }

    public record Key(String id, Path file) {
    }

    public Key issue(String purpose, boolean healthPermission, int duration) throws Exception {
        var name = "ch03_" + purpose + "_" + java.util.UUID.randomUUID().toString().replace("-", "");
        var permissions = healthPermission ? java.util.List.of("cluster:monitor/main", "cluster:monitor/health")
                : java.util.List.of("cluster:monitor/main");
        var json = JsonMapper.builder().build();
        var body = json.writeValueAsString(java.util.Map.of("name", name, "cluster_permissions", permissions,
                "index_permissions", java.util.List.of(), "duration_seconds", duration));
        var response = admin("POST", "/_plugins/_security/api/apitokens", body);
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new IllegalStateException("API key creation failed: HTTP " + response.statusCode());
        }
        var tree = json.readTree(response.body());
        var token = tree.path("token").asString();
        var id = tree.path("id").asString();
        if (!token.startsWith("os_") || id.isBlank()) {
            throw new IllegalStateException("API key creation returned an unexpected schema");
        }
        var file = material.resolve("secrets/" + name + ".token");
        Files.writeString(file, token);
        return new Key(id, file);
    }

    public void revoke(Key key) throws Exception {
        if (admin("DELETE", "/_plugins/_security/api/apitokens/" + key.id(), null).statusCode() != 200) {
            throw new IllegalStateException("API key revocation failed");
        }
    }

    public HttpResponse<String> keyed(Key key, String method, String path) throws Exception {
        try (var client = HttpClient.newBuilder().sslContext(TestTlsContexts.create(material.resolve("public/ca.pem")))
                .connectTimeout(Duration.ofSeconds(2)).build()) {
            var request = HttpRequest.newBuilder(uri().resolve(path)).timeout(Duration.ofSeconds(5))
                    .header("Authorization", "ApiKey " + Files.readString(key.file()))
                    .method(method, HttpRequest.BodyPublishers.noBody()).build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    public HttpResponse<String> admin(String method, String path, String body) throws Exception {
        var certificates = CertificateFactory.getInstance("X.509");
        Certificate adminCertificate;
        Certificate ca;
        try (var input = Files.newInputStream(material.resolve("admin/admin.pem"))) {
            adminCertificate = certificates.generateCertificate(input);
        }
        try (var input = Files.newInputStream(material.resolve("public/ca.pem"))) {
            ca = certificates.generateCertificate(input);
        }
        var pem = Files.readString(material.resolve("admin/admin.key"))
                .replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
        var key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
        var store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        store.setKeyEntry("admin", key, new char[0], new Certificate[]{adminCertificate, ca});
        var keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(store, new char[0]);
        var trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", ca);
        var trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(trustStore);
        var ssl = SSLContext.getInstance("TLS");
        ssl.init(keys.getKeyManagers(), trust.getTrustManagers(), null);
        try (var client = HttpClient.newBuilder().sslContext(ssl).connectTimeout(Duration.ofSeconds(2)).build()) {
            var request = HttpRequest.newBuilder(uri().resolve(path)).timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    public URI uri() {
        return URI.create("https://" + node.getHost() + ":" + node.getMappedPort(9200));
    }

    public Path material() {
        return material;
    }

    public record ApplicationSettings(URI uri, Path apiKeyFile) {
    }

    public ApplicationSettings properties(Path key) {
        return new ApplicationSettings(uri(), key);
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
