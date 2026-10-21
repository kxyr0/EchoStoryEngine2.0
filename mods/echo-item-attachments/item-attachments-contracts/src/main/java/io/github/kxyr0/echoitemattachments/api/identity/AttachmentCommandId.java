package io.github.kxyr0.echoitemattachments.api.identity;

import java.util.Objects;

public record AttachmentCommandId(String value) implements Comparable<AttachmentCommandId> {
    public AttachmentCommandId {
        value = Objects.requireNonNull(value, "command id").trim();
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("command id must be namespaced: " + value);
        }
    }

    @Override
    public int compareTo(AttachmentCommandId other) {
        return value.compareTo(other.value);
    }
}
