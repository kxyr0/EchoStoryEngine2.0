package io.github.kxyr0.echonpcinteraction.api.result;

import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionProviderId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record NpcInteractionReceipt(
        NpcInteractionCommandId commandId,
        ActorId npcId,
        NpcInteractionProviderId providerId,
        BlockPosition targetPosition,
        BlockId blockId,
        String stateFingerprintBefore,
        String interactionResult,
        String stateFingerprintAfter,
        Optional<String> seatAttachmentId,
        long executionTick,
        String verificationPolicy,
        NpcInteractionOutcome outcome,
        List<NpcInteractionConsequenceKind> consequenceKinds,
        Optional<String> spawnedEntityIds,
        Optional<String> proxyVehicleBefore,
        Optional<String> proxyVehicleAfter,
        Optional<NpcRideHandoffStrategy> rideHandoffStrategy,
        Optional<String> echoSeatAnchorId,
        Optional<String> seatOccupancyId,
        boolean npcPassengerConfirmed,
        boolean proxyCleanupSucceeded,
        boolean foreignVehicleCleanupSucceeded,
        long verificationRevision,
        long receiptRevision) {
    public NpcInteractionReceipt(
            NpcInteractionCommandId commandId,
            ActorId npcId,
            NpcInteractionProviderId providerId,
            BlockPosition targetPosition,
            BlockId blockId,
            String stateFingerprintBefore,
            String interactionResult,
            String stateFingerprintAfter,
            Optional<String> seatAttachmentId,
            long executionTick,
            String verificationPolicy,
            NpcInteractionOutcome outcome,
            long receiptRevision) {
        this(commandId, npcId, providerId, targetPosition, blockId, stateFingerprintBefore, interactionResult,
                stateFingerprintAfter, seatAttachmentId, executionTick, verificationPolicy, outcome,
                List.of(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), false, true, true, receiptRevision, receiptRevision);
    }

    public NpcInteractionReceipt {
        commandId = Objects.requireNonNull(commandId, "commandId");
        npcId = Objects.requireNonNull(npcId, "npcId");
        providerId = Objects.requireNonNull(providerId, "providerId");
        targetPosition = Objects.requireNonNull(targetPosition, "targetPosition");
        blockId = Objects.requireNonNull(blockId, "blockId");
        stateFingerprintBefore = text(stateFingerprintBefore, "stateFingerprintBefore");
        interactionResult = text(interactionResult, "interactionResult");
        stateFingerprintAfter = text(stateFingerprintAfter, "stateFingerprintAfter");
        seatAttachmentId = Objects.requireNonNull(seatAttachmentId, "seatAttachmentId");
        verificationPolicy = text(verificationPolicy, "verificationPolicy");
        outcome = Objects.requireNonNull(outcome, "outcome");
        consequenceKinds = List.copyOf(Objects.requireNonNull(consequenceKinds, "consequenceKinds"));
        spawnedEntityIds = Objects.requireNonNull(spawnedEntityIds, "spawnedEntityIds");
        proxyVehicleBefore = Objects.requireNonNull(proxyVehicleBefore, "proxyVehicleBefore");
        proxyVehicleAfter = Objects.requireNonNull(proxyVehicleAfter, "proxyVehicleAfter");
        rideHandoffStrategy = Objects.requireNonNull(rideHandoffStrategy, "rideHandoffStrategy");
        echoSeatAnchorId = Objects.requireNonNull(echoSeatAnchorId, "echoSeatAnchorId");
        seatOccupancyId = Objects.requireNonNull(seatOccupancyId, "seatOccupancyId");
        if (executionTick < 0L || receiptRevision < 1L || verificationRevision < 1L) {
            throw new IllegalArgumentException("receipt revisions must be positive.");
        }
    }

    private static String text(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must be non-empty.");
        }
        return checked;
    }
}
