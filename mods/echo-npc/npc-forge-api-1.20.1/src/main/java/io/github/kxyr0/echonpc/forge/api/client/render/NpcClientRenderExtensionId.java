package io.github.kxyr0.echonpc.forge.api.client.render;

import java.util.Locale;
import java.util.Objects;

/**
 * Stable public client render extension identity.
 */
public record NpcClientRenderExtensionId(String value) implements Comparable<NpcClientRenderExtensionId> {
    public NpcClientRenderExtensionId {
        value = require(value);
    }

    @Override
    public int compareTo(NpcClientRenderExtensionId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }

    private static String require(String raw) {
        String checked = Objects.requireNonNull(raw, "value").trim().toLowerCase(Locale.ROOT);
        int separator = checked.indexOf(':');
        if (separator < 1 || separator != checked.lastIndexOf(':') || separator == checked.length() - 1) {
            throw new IllegalArgumentException("NpcClientRenderExtensionId must be namespaced.");
        }
        if (!checked.substring(0, separator).matches("[a-z0-9_.-]+")
                || !checked.substring(separator + 1).matches("[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("NpcClientRenderExtensionId contains invalid characters.");
        }
        return checked;
    }
}
