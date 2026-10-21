package io.github.kxyr0.echonpcbackpack.forge.marker;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackMarkerId;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackCommandResult;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackAttachmentPoint;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackOutcome;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackStatus;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPhase;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPolicy;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionType;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import io.github.kxyr0.echonpcbackpack.core.transition.BackpackMarkerSequence;
import io.github.kxyr0.echonpcbackpack.forge.runtime.BackpackRuntimeState;
import io.github.kxyr0.echonpcbackpack.forge.state.BackpackActorRecord;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.core.BlockPos;

public final class BackpackMarkerCoordinator {
    private final BackpackRuntimeState state;

    public BackpackMarkerCoordinator(BackpackRuntimeState state) {
        this.state = state;
    }

    public NpcBackpackCommandResult accept(ActorId actor, NpcBackpackMarkerId marker, String occurrenceId) {
        BackpackActorRecord record = state.actors.get(actor.value());
        NpcBackpackCommandId command = record == null ? fallback(actor) : record.commandId.orElse(fallback(actor));
        if (record == null || !record.transitioning()
                || record.policy != NpcBackpackTransitionPolicy.ANIMATION_MARKERS) {
            return result(NpcBackpackOutcome.REJECTED, command, record,
                    "ENPC-SB-NO-MARKER-TRANSITION", "NPC has no animation-marker backpack transition.");
        }
        String receipt = marker.value() + "@" + occurrenceId;
        if (record.markerReceipts.contains(receipt)) {
            return result(NpcBackpackOutcome.ACCEPTED, command, record,
                    "ENPC-SB-MARKER-DUPLICATE", "Duplicate marker delivery ignored.");
        }
        try {
            return apply(record, marker.value(), receipt);
        } catch (IOException exception) {
            return recovery(record, "ENPC-SB-PERSISTENCE-FAILED", exception.getMessage());
        }
    }

    private NpcBackpackCommandResult apply(BackpackActorRecord record, String marker,
            String receipt) throws IOException {
        if (BackpackMarkerSequence.hand(marker)) return hand(record, marker, receipt);
        if (record.transitionType.orElseThrow() == NpcBackpackTransitionType.EQUIP) {
            return equip(record, marker, receipt);
        }
        return unequip(record, marker, receipt);
    }

    private NpcBackpackCommandResult equip(BackpackActorRecord record, String marker,
            String receipt) throws IOException {
        if (BackpackMarkerSequence.TAKE.equals(marker)
                && record.phase.orElseThrow() == NpcBackpackTransitionPhase.READY_TO_TAKE) {
            record.markerReceipts.add(receipt);
            var mutation = state.mutations.extract(record, state.level(record.source.orElseThrow()).orElseThrow(),
                    block(record.source.orElseThrow()), state::persist);
            return mutation.succeeded() ? accepted(record, "Backpack taken from world.")
                    : recovery(record, mutation.code(), mutation.message());
        }
        if (BackpackMarkerSequence.BACK.equals(marker) && record.carryingPayload()
                && received(record, BackpackMarkerSequence.TAKE)) {
            record.markerReceipts.add(receipt);
            record.attachmentPoint = NpcBackpackAttachmentPoint.BACK;
            record.status = NpcBackpackStatus.EQUIPPED;
            return complete(record, "Backpack attached to back.");
        }
        return outOfOrder(record);
    }

    private NpcBackpackCommandResult unequip(BackpackActorRecord record, String marker,
            String receipt) throws IOException {
        if (BackpackMarkerSequence.DETACH.equals(marker)
                && record.phase.orElseThrow() == NpcBackpackTransitionPhase.ATTACHED_TO_BACK) {
            record.markerReceipts.add(receipt);
            record.attachmentPoint = NpcBackpackAttachmentPoint.RIGHT_HAND;
            record.phase = Optional.of(NpcBackpackTransitionPhase.READY_TO_PLACE);
            record.revision++;
            state.persist();
            return accepted(record, "Backpack detached from back.");
        }
        if (BackpackMarkerSequence.PLACE.equals(marker) && received(record, BackpackMarkerSequence.DETACH)) {
            record.markerReceipts.add(receipt);
            state.persist();
            var body = state.body(record.actorId).orElseThrow();
            var mutation = state.mutations.place(record, body.level(), block(record.destination.orElseThrow()),
                    body.mob().getDirection(), state::persist);
            return mutation.succeeded() ? complete(record, mutation.message())
                    : recovery(record, mutation.code(), mutation.message());
        }
        return outOfOrder(record);
    }

    private NpcBackpackCommandResult hand(BackpackActorRecord record, String marker, String receipt)
            throws IOException {
        boolean afterTake = record.transitionType.orElseThrow() == NpcBackpackTransitionType.EQUIP
                && received(record, BackpackMarkerSequence.TAKE) && record.carryingPayload();
        boolean afterDetach = record.transitionType.orElseThrow() == NpcBackpackTransitionType.UNEQUIP
                && received(record, BackpackMarkerSequence.DETACH);
        if (!afterTake && !afterDetach) return outOfOrder(record);
        record.markerReceipts.add(receipt);
        record.attachmentPoint = BackpackMarkerSequence.LEFT_HAND.equals(marker)
                ? NpcBackpackAttachmentPoint.LEFT_HAND : NpcBackpackAttachmentPoint.RIGHT_HAND;
        record.phase = Optional.of(NpcBackpackTransitionPhase.HELD_IN_HAND);
        record.revision++;
        state.persist();
        return accepted(record, "Backpack attached to hand.");
    }

    private NpcBackpackCommandResult complete(BackpackActorRecord record, String message) throws IOException {
        NpcBackpackCommandId command = record.commandId.orElseThrow();
        record.phase = Optional.of(NpcBackpackTransitionPhase.COMPLETED);
        record.status = record.carryingPayload() ? NpcBackpackStatus.EQUIPPED : NpcBackpackStatus.NONE;
        record.diagnosticCode = "ENPC-SB-OK";
        record.diagnosticMessage = message;
        record.revision++;
        state.terminalCommands.put(command.value(), NpcBackpackOutcome.SUCCEEDED.name());
        trimLedger();
        state.persist();
        record.clearTransition();
        if (!record.carryingPayload()) record.expectedFingerprint = Optional.empty();
        record.revision++;
        state.persist();
        return result(NpcBackpackOutcome.SUCCEEDED, command, record, "ENPC-SB-SUCCEEDED", message);
    }

    private NpcBackpackCommandResult recovery(BackpackActorRecord record, String code, String message) {
        record.status = NpcBackpackStatus.RECOVERY_REQUIRED;
        record.phase = Optional.of(NpcBackpackTransitionPhase.RECOVERY_REQUIRED);
        record.diagnosticCode = code;
        record.diagnosticMessage = message;
        record.revision++;
        try { state.persist(); } catch (IOException ignored) { }
        return result(NpcBackpackOutcome.RECOVERY_REQUIRED, record.commandId.orElse(fallback(record.actorId)),
                record, code, message);
    }

    private NpcBackpackCommandResult outOfOrder(BackpackActorRecord record) {
        return result(NpcBackpackOutcome.REJECTED, record.commandId.orElseThrow(), record,
                "ENPC-SB-MARKER-OUT-OF-ORDER", "Marker is not valid in the current transition phase.");
    }

    private NpcBackpackCommandResult accepted(BackpackActorRecord record, String message) {
        return result(NpcBackpackOutcome.ACCEPTED, record.commandId.orElseThrow(), record,
                "ENPC-SB-MARKER-ACCEPTED", message);
    }

    private boolean received(BackpackActorRecord record, String marker) {
        return record.markerReceipts.stream().anyMatch(value -> value.startsWith(marker + "@"));
    }

    private void trimLedger() {
        while (state.terminalCommands.size() > 1024) {
            state.terminalCommands.remove(state.terminalCommands.keySet().iterator().next());
        }
    }

    private NpcBackpackCommandId fallback(ActorId actor) {
        return new NpcBackpackCommandId("echo_npc_sophisticated_backpacks:query_"
                + Integer.toUnsignedString(actor.value().hashCode(), 36));
    }

    private BlockPos block(BackpackBlockLocation location) {
        return new BlockPos(location.position().x(), location.position().y(), location.position().z());
    }

    private NpcBackpackCommandResult result(NpcBackpackOutcome outcome, NpcBackpackCommandId command,
            BackpackActorRecord record, String code, String message) {
        return new NpcBackpackCommandResult(outcome, command,
                record == null ? Optional.empty() : Optional.of(state.views.view(record)), code, message);
    }
}
