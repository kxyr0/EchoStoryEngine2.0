package io.github.kxyr0.echoanimation.api.profile;

import java.util.Objects;

/**
 * Semantic fingerprint for profile rebind detection.
 */
public record AnimationProfileFingerprint(String value) {
    public AnimationProfileFingerprint {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty() || value.length() > 128) {
            throw new IllegalArgumentException("AnimationProfileFingerprint must be non-empty and at most 128 chars.");
        }
    }
}
