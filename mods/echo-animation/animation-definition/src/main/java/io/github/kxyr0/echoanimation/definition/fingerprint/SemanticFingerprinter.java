package io.github.kxyr0.echoanimation.definition.fingerprint;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

/**
 * Stable SHA-256 fingerprint over already-bound semantic fields.
 */
public final class SemanticFingerprinter {
    public String fingerprint(Collection<String> fields) {
        MessageDigest digest = sha256();
        fields.stream().sorted().forEach(field -> {
            digest.update(field.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
        });
        return toHex(digest.digest());
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 недоступен.", exception);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}

