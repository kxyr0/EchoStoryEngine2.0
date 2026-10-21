package io.github.kxyr0.echovisibility.forge.api;

import io.github.kxyr0.echovisibility.api.identity.VisibilityPlayerRef;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import io.github.kxyr0.echovisibility.api.report.VisibilityTransition;
import io.github.kxyr0.echovisibility.api.state.VisibilityState;
import io.github.kxyr0.echovisibility.api.state.VisibilityWatchStatus;
import java.util.Objects;
import java.util.Optional;

public record VisibilityWatchView(
        VisibilityPlayerRef player,
        VisibilityWatchDefinition definition,
        VisibilityWatchGeneration generation,
        String ownerLineage,
        VisibilityWatchStatus status,
        VisibilityState state,
        Optional<VisibilityTransition> lastTransition,
        long lastReportSequence,
        long lastAcceptedServerTick) {
    public VisibilityWatchView {
        player = Objects.requireNonNull(player, "player");
        definition = Objects.requireNonNull(definition, "definition");
        generation = Objects.requireNonNull(generation, "generation");
        ownerLineage = Objects.requireNonNull(ownerLineage, "ownerLineage");
        status = Objects.requireNonNull(status, "status");
        state = Objects.requireNonNull(state, "state");
        lastTransition = Objects.requireNonNull(lastTransition, "lastTransition");
    }
}
