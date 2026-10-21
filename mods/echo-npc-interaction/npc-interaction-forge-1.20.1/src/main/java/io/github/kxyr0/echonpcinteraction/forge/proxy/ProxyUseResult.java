package io.github.kxyr0.echonpcinteraction.forge.proxy;

import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionConsequenceKind;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionFailure;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionOutcome;
import io.github.kxyr0.echonpcinteraction.api.result.NpcRideHandoffStrategy;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ProxyUseResult(
        boolean success,
        String interactionResult,
        String beforeFingerprint,
        String afterFingerprint,
        Optional<NpcInteractionFailure> failure,
        NpcInteractionOutcome outcome,
        List<NpcInteractionConsequenceKind> consequences,
        Optional<String> spawnedEntityIds,
        Optional<String> proxyVehicleBefore,
        Optional<String> proxyVehicleAfter,
        Optional<NpcRideHandoffStrategy> rideHandoffStrategy,
        Optional<String> echoSeatAnchorId,
        Optional<String> seatOccupancyId,
        boolean npcPassengerConfirmed,
        boolean proxyCleanupSucceeded,
        boolean foreignVehicleCleanupSucceeded,
        String message) {
    public ProxyUseResult {
        interactionResult = Objects.requireNonNull(interactionResult, "interactionResult");
        beforeFingerprint = Objects.requireNonNull(beforeFingerprint, "beforeFingerprint");
        afterFingerprint = Objects.requireNonNull(afterFingerprint, "afterFingerprint");
        failure = Objects.requireNonNull(failure, "failure");
        outcome = Objects.requireNonNull(outcome, "outcome");
        consequences = List.copyOf(Objects.requireNonNull(consequences, "consequences"));
        spawnedEntityIds = Objects.requireNonNull(spawnedEntityIds, "spawnedEntityIds");
        proxyVehicleBefore = Objects.requireNonNull(proxyVehicleBefore, "proxyVehicleBefore");
        proxyVehicleAfter = Objects.requireNonNull(proxyVehicleAfter, "proxyVehicleAfter");
        rideHandoffStrategy = Objects.requireNonNull(rideHandoffStrategy, "rideHandoffStrategy");
        echoSeatAnchorId = Objects.requireNonNull(echoSeatAnchorId, "echoSeatAnchorId");
        seatOccupancyId = Objects.requireNonNull(seatOccupancyId, "seatOccupancyId");
        message = Objects.requireNonNull(message, "message");
    }
}
