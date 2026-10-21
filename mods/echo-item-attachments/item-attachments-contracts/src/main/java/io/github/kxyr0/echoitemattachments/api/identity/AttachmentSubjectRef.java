package io.github.kxyr0.echoitemattachments.api.identity;

import java.util.Objects;

public record AttachmentSubjectRef(SubjectKind kind, String value)
        implements Comparable<AttachmentSubjectRef> {
    public AttachmentSubjectRef {
        kind = Objects.requireNonNull(kind, "kind");
        value = Objects.requireNonNull(value, "subject value").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("subject value must be non-empty");
    }

    public String externalKey() {
        return kind.name().toLowerCase(java.util.Locale.ROOT) + ":" + value;
    }

    @Override
    public int compareTo(AttachmentSubjectRef other) {
        int kindOrder = kind.compareTo(other.kind);
        return kindOrder != 0 ? kindOrder : value.compareTo(other.value);
    }
}
