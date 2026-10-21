package io.github.kxyr0.echovisibility.core.arm;

import io.github.kxyr0.echovisibility.api.identity.VisibilityPlayerRef;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import io.github.kxyr0.echovisibility.api.report.VisibilityTransition;
import io.github.kxyr0.echovisibility.api.state.VisibilityState;
import io.github.kxyr0.echovisibility.api.state.VisibilityWatchStatus;
import java.util.Objects;
import java.util.Optional;

public record VisibilityArmSnapshot(
        VisibilityPlayerRef player,
        VisibilityWatchDefinition definition,
        VisibilityWatchGeneration generation,
        String ownerLineage,
        long createdTick,
        long connectionGeneration,
        VisibilityWatchStatus status,
        VisibilityState lastState,
        Optional<VisibilityTransition> lastTransition,
        long lastAcceptedSequence,
        long lastAcceptedClientTick,
        long lastAcceptedServerTick) {
    public VisibilityArmSnapshot {
        player = Objects.requireNonNull(player, "player");
        definition = Objects.requireNonNull(definition, "definition");
        generation = Objects.requireNonNull(generation, "generation");
        ownerLineage = Objects.requireNonNull(ownerLineage, "ownerLineage");
        status = Objects.requireNonNull(status, "status");
        lastState = Objects.requireNonNull(lastState, "lastState");
        lastTransition = Objects.requireNonNull(lastTransition, "lastTransition");
    }
}
