package io.github.kxyr0.echoitem.api.source;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Stable content fingerprint used for incremental reload.
 */
public record ItemSourceFingerprint(String sha256) {
    public ItemSourceFingerprint {
        sha256 = Objects.requireNonNull(sha256, "sha256");
        if (!sha256.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Fingerprint must be lowercase SHA-256 hex.");
        }
    }

    public static ItemSourceFingerprint ofBytes(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return new ItemSourceFingerprint(HexFormat.of().formatHex(digest.digest(bytes)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required by Java.", ex);
        }
    }
}
