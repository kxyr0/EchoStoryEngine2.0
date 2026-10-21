package io.github.kxyr0.echotrigger.api.authoring.catalog;

import java.util.Objects;

public record AreaSourceLocation(String rootKind, String path) {
    public AreaSourceLocation {
        rootKind = Objects.requireNonNull(rootKind, "rootKind").trim();
        path = Objects.requireNonNull(path, "path").trim();
        if (rootKind.isEmpty() || path.isEmpty()) {
            throw new IllegalArgumentException("Source root kind and path are required.");
        }
    }
}
