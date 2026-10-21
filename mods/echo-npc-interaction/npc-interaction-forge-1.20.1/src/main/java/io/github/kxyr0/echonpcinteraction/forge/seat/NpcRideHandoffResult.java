package io.github.kxyr0.echonpcinteraction.forge.seat;

import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionFailure;
import io.github.kxyr0.echonpcinteraction.api.result.NpcRideHandoffStrategy;
import java.util.Objects;
import java.util.Optional;

public record NpcRideHandoffResult(
        boolean success,
        NpcRideHandoffStrategy strategy,
        Optional<NpcInteractionFailure> failure,
        String message,
        Optional<String> anchorId,
        Optional<String> occupancyId,
        boolean npcPassengerConfirmed,
        boolean proxyCleanupSucceeded,
        boolean foreignVehicleCleanupSucceeded) {
    public NpcRideHandoffResult {
        strategy = Objects.requireNonNull(strategy, "strategy");
        failure = Objects.requireNonNull(failure, "failure");
        message = Objects.requireNonNull(message, "message");
        anchorId = Objects.requireNonNull(anchorId, "anchorId");
        occupancyId = Objects.requireNonNull(occupancyId, "occupancyId");
    }

    static NpcRideHandoffResult failed(NpcRideHandoffStrategy strategy, NpcInteractionFailure failure,
            String message) {
        return new NpcRideHandoffResult(false, strategy, Optional.of(failure), message,
                Optional.empty(), Optional.empty(), false, true, true);
    }
}
