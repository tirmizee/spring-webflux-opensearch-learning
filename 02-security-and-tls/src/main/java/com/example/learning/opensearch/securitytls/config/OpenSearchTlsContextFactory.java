package com.example.learning.opensearch.securitytls.config;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;

public final class OpenSearchTlsContextFactory {

    private OpenSearchTlsContextFactory() {
    }

    public static SSLContext create(Path caCertificate) throws IOException, GeneralSecurityException {
        // ใช้ trust anchors ของบทเท่านั้น ไม่เปลี่ยน JVM global truststore
        var store = KeyStore.getInstance(KeyStore.getDefaultType());
        store.load(null, null);
        try (var input = Files.newInputStream(caCertificate)) {
            var certificates = CertificateFactory.getInstance("X.509").generateCertificates(input);
            if (certificates.isEmpty()) {
                throw new GeneralSecurityException("CA certificate file contains no certificates");
            }
            int index = 0;
            for (var certificate : certificates) {
                store.setCertificateEntry("ca-" + index++, certificate);
            }
        }
        var trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(store);
        var context = SSLContext.getInstance("TLS");
        context.init(null, trust.getTrustManagers(), null);
        return context;
    }
}
