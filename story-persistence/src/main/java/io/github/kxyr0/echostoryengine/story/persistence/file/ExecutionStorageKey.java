package io.github.kxyr0.echostoryengine.story.persistence.file;

import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class ExecutionStorageKey {
    ExecutionFileSet files(Path rootDirectory, ExecutionId executionId) {
        String key = hash(executionId);
        return new ExecutionFileSet(
                rootDirectory.resolve(key + ".json"),
                rootDirectory.resolve(key + ".bak"),
                rootDirectory.resolve(key + ".tmp"));
    }

    private String hash(ExecutionId executionId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(executionId.asString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }
}
