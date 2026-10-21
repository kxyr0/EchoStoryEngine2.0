package io.github.kxyr0.echodevscenarios.api;

import java.util.Objects;

public record ScenarioTarget(Kind kind, String resourceId) {
    public ScenarioTarget {
        kind = Objects.requireNonNull(kind, "kind");
        resourceId = Objects.requireNonNull(resourceId, "resourceId");
    }

    public enum Kind {
        SCENE,
        STORY
    }
}
