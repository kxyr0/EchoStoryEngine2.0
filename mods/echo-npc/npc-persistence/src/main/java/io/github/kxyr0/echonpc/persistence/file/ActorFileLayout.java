package io.github.kxyr0.echonpc.persistence.file;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Stable world-local file layout for actor records.
 */
public final class ActorFileLayout {
    private final Path root;

    public ActorFileLayout(Path root) {
        this.root = java.util.Objects.requireNonNull(root, "root");
    }

    public Path root() {
        return root;
    }

    public Path primary(ActorId actorId) {
        return root.resolve(storageKey(actorId) + ".json");
    }

    public Path backup(ActorId actorId) {
        return root.resolve(storageKey(actorId) + ".json.bak");
    }

    public Path temp(ActorId actorId) {
        return root.resolve(storageKey(actorId) + ".json.tmp");
    }

    public boolean matches(Path file, ActorId actorId) {
        return file.getFileName().toString().equals(storageKey(actorId) + ".json");
    }

    public String storageKey(ActorId actorId) {
        return sha256(actorId.value());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
