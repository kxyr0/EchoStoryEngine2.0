package io.github.kxyr0.echodialogue.persistence.file;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Safe deterministic session file names under one repository root.
 */
final class SessionFileLayout {
    private final Path root;

    SessionFileLayout(Path root) {
        this.root = root;
    }

    Path primary(DialogueSessionId sessionId) {
        return root.resolve(baseName(sessionId) + ".json");
    }

    Path backup(DialogueSessionId sessionId) {
        return root.resolve(baseName(sessionId) + ".bak");
    }

    Path temp(DialogueSessionId sessionId) {
        return root.resolve(baseName(sessionId) + ".tmp");
    }

    Path backupForPrimary(Path primary) {
        return replaceExtension(primary, ".bak");
    }

    Path tempForPrimary(Path primary) {
        return replaceExtension(primary, ".tmp");
    }

    private Path replaceExtension(Path primary, String extension) {
        String name = primary.getFileName().toString().replaceFirst("\\.json$", extension);
        return primary.resolveSibling(name);
    }

    private String baseName(DialogueSessionId sessionId) {
        return "session-" + sha256(sessionId.toString());
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 недоступен в JVM.", exception);
        }
    }
}
