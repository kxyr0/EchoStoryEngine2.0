package io.github.kxyr0.echostoryengine.story.api.identity;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Deterministic compatibility token for one compiled story definition.
 */
public record CompiledStoryFingerprint(String value) {
    private static final Pattern HEX_SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public CompiledStoryFingerprint {
        value = Objects.requireNonNull(value, "value").trim();
        if (!HEX_SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException("fingerprint must be lowercase SHA-256 hex");
        }
    }
}
