package io.github.kxyr0.echovisibility.core.arm;

import io.github.kxyr0.echovisibility.api.identity.VisibilityPlayerRef;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import java.util.Objects;

public record VisibilityGenerationSnapshot(
        VisibilityPlayerRef player,
        VisibilityWatchId watchId,
        VisibilityWatchGeneration generation) {
    public VisibilityGenerationSnapshot {
        player = Objects.requireNonNull(player, "player");
        watchId = Objects.requireNonNull(watchId, "watchId");
        generation = Objects.requireNonNull(generation, "generation");
    }
}
