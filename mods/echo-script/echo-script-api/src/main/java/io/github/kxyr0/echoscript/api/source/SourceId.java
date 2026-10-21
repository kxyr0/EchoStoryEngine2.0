package io.github.kxyr0.echoscript.api.source;

import java.util.Objects;

/**
 * Immutable logical source identity.
 *
 * <p>The value is platform-neutral and must not be treated as a filesystem path. Empty values are invalid.</p>
 */
public record SourceId(String value) {
    public SourceId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("SourceId must not be empty.");
        }
    }
}
