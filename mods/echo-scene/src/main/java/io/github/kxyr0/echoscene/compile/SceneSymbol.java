package io.github.kxyr0.echoscene.compile;

import java.util.Objects;

public record SceneSymbol(String name, String resourceId, SceneSymbolKind kind) {
    public SceneSymbol {
        name = Objects.requireNonNull(name, "name");
        resourceId = Objects.requireNonNull(resourceId, "resourceId");
        kind = Objects.requireNonNull(kind, "kind");
    }
}
