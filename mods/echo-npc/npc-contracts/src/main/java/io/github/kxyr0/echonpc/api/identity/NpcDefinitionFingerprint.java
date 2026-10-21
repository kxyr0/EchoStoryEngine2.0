package io.github.kxyr0.echonpc.api.identity;

import java.util.Objects;

/**
 * Deterministic semantic fingerprint of a compiled NPC definition.
 */
public record NpcDefinitionFingerprint(String value) {
    public NpcDefinitionFingerprint {
        value = Objects.requireNonNull(value, "value").trim().toLowerCase(java.util.Locale.ROOT);
        if (!value.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("NpcDefinitionFingerprint must be a 64-character SHA-256 hex value.");
        }
    }

    public String abbreviation() {
        return value.substring(0, 12);
    }

    @Override
    public String toString() {
        return value;
    }
}
