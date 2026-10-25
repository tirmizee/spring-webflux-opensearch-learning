package com.example.learning.opensearch.securitytls.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class OpenSearchPasswordFile {

    private OpenSearchPasswordFile() {
    }

    static String read(Path path) throws IOException {
        // อ่านตอน startup; ตัด final LF/CRLF หนึ่งชุดโดยคง password spaces
        var password = Files.readString(path).replaceFirst("\\r?\\n\\z", "");
        if (password.isBlank() || password.contains("\n") || password.contains("\r")) {
            throw new IllegalArgumentException("Password file must contain one nonblank line");
        }
        return password;
    }
}
