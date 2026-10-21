package io.github.kxyr0.echodialogue.core.fingerprint;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable hash of canonical compiled dialogue meaning.
 */
public record CompiledDialogueFingerprint(String value) {
    private static final Pattern HEX_256 = Pattern.compile("[a-f0-9]{64}");

    public CompiledDialogueFingerprint {
        value = Objects.requireNonNull(value, "value").trim();
        if (!HEX_256.matcher(value).matches()) {
            throw new IllegalArgumentException("Compiled dialogue fingerprint must be lowercase SHA-256 hex.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
