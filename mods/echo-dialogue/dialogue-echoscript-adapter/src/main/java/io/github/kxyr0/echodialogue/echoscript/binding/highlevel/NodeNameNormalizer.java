package io.github.kxyr0.echodialogue.echoscript.binding.highlevel;

import java.util.Locale;
import java.util.Objects;

/**
 * Single deterministic source-symbol to dialogue-node ID normalization rule.
 */
public final class NodeNameNormalizer {
    private NodeNameNormalizer() {
    }

    public static String normalize(String sourceName) {
        String value = Objects.requireNonNull(sourceName, "sourceName")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("([A-Za-z])([0-9])", "$1_$2")
                .replaceAll("([0-9])([A-Za-z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Dialogue node name cannot normalize to an empty ID.");
        }
        return value;
    }
}
