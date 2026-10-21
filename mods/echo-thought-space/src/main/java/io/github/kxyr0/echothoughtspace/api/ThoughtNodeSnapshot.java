package io.github.kxyr0.echothoughtspace.api;

import java.util.Objects;

public record ThoughtNodeSnapshot(
        ThoughtNodeId id,
        ThoughtNodeLabel label,
        ThoughtNodeRevision revision) {
    public ThoughtNodeSnapshot {
        id = Objects.requireNonNull(id, "id");
        label = Objects.requireNonNull(label, "label");
        revision = Objects.requireNonNull(revision, "revision");
    }
}

