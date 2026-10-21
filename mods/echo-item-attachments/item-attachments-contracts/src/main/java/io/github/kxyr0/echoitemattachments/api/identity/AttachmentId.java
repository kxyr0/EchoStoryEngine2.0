package io.github.kxyr0.echoitemattachments.api.identity;

import java.util.Objects;

public record AttachmentId(String value) implements Comparable<AttachmentId> {
    public AttachmentId {
        value = namespaced(value, "attachment id");
    }

    @Override
    public int compareTo(AttachmentId other) {
        return value.compareTo(other.value);
    }

    private static String namespaced(String value, String label) {
        String normalized = Objects.requireNonNull(value, label).trim();
        if (!normalized.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException(label + " must be namespaced: " + normalized);
        }
        return normalized;
    }
}
