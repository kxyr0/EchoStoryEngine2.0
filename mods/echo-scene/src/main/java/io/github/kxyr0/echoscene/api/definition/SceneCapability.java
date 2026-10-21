package io.github.kxyr0.echoscene.api.definition;

import java.util.Objects;

public record SceneCapability(SceneCapabilityKind kind, String resource) implements Comparable<SceneCapability> {
    public SceneCapability {
        kind = Objects.requireNonNull(kind, "kind");
        resource = Objects.requireNonNull(resource, "resource").trim();
        if (resource.isEmpty()) {
            throw new IllegalArgumentException("Capability resource must not be empty.");
        }
    }

    @Override
    public int compareTo(SceneCapability other) {
        int byKind = Integer.compare(kind.order(), other.kind.order());
        return byKind == 0 ? resource.compareTo(other.resource) : byKind;
    }
}
