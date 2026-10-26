package com.example.learning.opensearch.apikey.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class OpenSearchApiKeyFile {

    private OpenSearchApiKeyFile() {
    }

    static String read(Path path) throws IOException {
        var token = Files.readString(path).replaceFirst("\\r?\\n\\z", "");
        if (token.isBlank() || token.length() > 4096 || !token.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("API key file must contain one nonblank opaque token without whitespace");
        }
        return token;
    }
}
