package io.github.kxyr0.echobackgroundnpc.api.definition;

import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearanceId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearancePoolId;
import java.util.List;
import java.util.Objects;

public record BackgroundAppearancePool(
        BackgroundAppearancePoolId id,
        List<BackgroundAppearanceId> appearances,
        long revision
) {
    public BackgroundAppearancePool {
        id = Objects.requireNonNull(id, "id");
        appearances = List.copyOf(Objects.requireNonNull(appearances, "appearances"));
        if (appearances.isEmpty() || revision < 0L) throw new IllegalArgumentException("Invalid appearance pool.");
    }
}
