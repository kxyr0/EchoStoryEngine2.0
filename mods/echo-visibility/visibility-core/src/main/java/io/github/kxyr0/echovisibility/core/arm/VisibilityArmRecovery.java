package io.github.kxyr0.echovisibility.core.arm;

import io.github.kxyr0.echovisibility.api.identity.VisibilityPlayerRef;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import java.util.Objects;

public record VisibilityArmRecovery(
        VisibilityPlayerRef player,
        VisibilityWatchDefinition definition,
        VisibilityWatchGeneration generation,
        String ownerLineage,
        long createdTick,
        long connectionGeneration) {
    public VisibilityArmRecovery {
        player = Objects.requireNonNull(player, "player");
        definition = Objects.requireNonNull(definition, "definition");
        generation = Objects.requireNonNull(generation, "generation");
        ownerLineage = Objects.requireNonNull(ownerLineage, "ownerLineage");
    }
}
