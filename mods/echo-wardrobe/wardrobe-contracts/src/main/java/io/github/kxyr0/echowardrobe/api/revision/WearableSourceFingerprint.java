package io.github.kxyr0.echowardrobe.api.revision;

import java.util.Objects;

/**
 * Stable fingerprint of authored wearable source text or asset bytes.
 */
public record WearableSourceFingerprint(String value) {
    public WearableSourceFingerprint {
        value = Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Source fingerprint must not be blank.");
        }
    }

    public static WearableSourceFingerprint builtin(String label) {
        return new WearableSourceFingerprint("builtin:" + Objects.requireNonNull(label, "label"));
    }
}
