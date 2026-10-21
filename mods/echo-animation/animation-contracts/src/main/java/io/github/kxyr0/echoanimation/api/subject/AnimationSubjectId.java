package io.github.kxyr0.echoanimation.api.subject;

import java.util.Locale;
import java.util.Objects;

/**
 * Neutral logical animation subject identity, independent from NPC identity.
 */
public record AnimationSubjectId(String value) implements Comparable<AnimationSubjectId> {
    private static final int MAX_LENGTH = 128;

    public AnimationSubjectId {
        value = require(value);
    }

    @Override
    public int compareTo(AnimationSubjectId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }

    private static String require(String raw) {
        String checked = Objects.requireNonNull(raw, "value").trim();
        if (checked.isEmpty() || checked.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("AnimationSubjectId must be non-empty and at most 128 characters.");
        }
        int separator = checked.indexOf(':');
        if (separator < 1 || separator != checked.lastIndexOf(':') || separator == checked.length() - 1) {
            throw new IllegalArgumentException("AnimationSubjectId must be namespaced as namespace:path.");
        }
        requireNamespace(checked.substring(0, separator));
        requirePath(checked.substring(separator + 1));
        return checked.toLowerCase(Locale.ROOT);
    }

    private static void requireNamespace(String value) {
        if (!value.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("AnimationSubjectId namespace contains invalid characters.");
        }
    }

    private static void requirePath(String value) {
        if (!value.matches("[a-z0-9_./-]+") || value.contains("//") || value.startsWith("/") || value.endsWith("/")) {
            throw new IllegalArgumentException("AnimationSubjectId path contains invalid characters.");
        }
    }
}
