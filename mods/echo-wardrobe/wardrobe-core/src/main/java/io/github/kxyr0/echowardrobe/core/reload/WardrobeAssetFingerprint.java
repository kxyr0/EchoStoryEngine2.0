package io.github.kxyr0.echowardrobe.core.reload;

import io.github.kxyr0.echowardrobe.api.revision.WearableSourceFingerprint;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Fingerprints immutable source or asset bytes.
 */
public final class WardrobeAssetFingerprint {
    private WardrobeAssetFingerprint() {
    }

    public static WearableSourceFingerprint sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return new WearableSourceFingerprint("sha256:" + hex(digest.digest(bytes)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JDK.", exception);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
