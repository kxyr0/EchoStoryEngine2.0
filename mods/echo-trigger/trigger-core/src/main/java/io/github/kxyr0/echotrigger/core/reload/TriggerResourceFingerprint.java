package io.github.kxyr0.echotrigger.core.reload;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public record TriggerResourceFingerprint(String value) {
    public TriggerResourceFingerprint {
        value = Objects.requireNonNull(value, "value");
    }

    public static TriggerResourceFingerprint sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return new TriggerResourceFingerprint(HexFormat.of().formatHex(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime.", exception);
        }
    }
}
