package io.github.kxyr0.echobackgroundnpc.api.identity.agent;

import java.util.Locale;
import java.util.Objects;

/** Stable authored identity of one background person. */
public record BackgroundNpcId(String value) implements Comparable<BackgroundNpcId> {
    public BackgroundNpcId { value = require(value); }

    @Override public int compareTo(BackgroundNpcId other) { return value.compareTo(other.value); }
    @Override public String toString() { return value; }

    private static String require(String raw) {
        String value = Objects.requireNonNull(raw, "value").trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("BackgroundNpcId must be namespaced.");
        }
        return value;
    }
}
