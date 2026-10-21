package io.github.kxyr0.echonpcbackpack.forge.runtime;

import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBody;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackAttachmentPoint;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackOutcome;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackStatus;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPhase;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPolicy;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionType;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import io.github.kxyr0.echonpcbackpack.forge.approach.BackpackApproachCoordinator;
import io.github.kxyr0.echonpcbackpack.forge.state.BackpackActorRecord;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.core.BlockPos;

public final class BackpackTransitionTicker {
    private final BackpackRuntimeState state;

    public BackpackTransitionTicker(BackpackRuntimeState state) {
        this.state = state;
    }

    public void tick() {
        state.actors.values().stream().filter(BackpackActorRecord::transitioning).limit(64).toList()
                .forEach(this::advance);
    }

    private void advance(BackpackActorRecord record) {
        if (record.phase.filter(value -> value == NpcBackpackTransitionPhase.APPROACHING).isPresent()) {
            var result = state.approach.poll(state.server(), record.navigationMoveId.orElseThrow());
            if (result.status() == BackpackApproachCoordinator.Status.ARRIVED) change(record,
                    NpcBackpackTransitionPhase.ALIGNING, "NPC arrived at backpack interaction point.");
            else if (result.status() == BackpackApproachCoordinator.Status.BLOCKED
                    || result.status() == BackpackApproachCoordinator.Status.UNAVAILABLE) {
                fail(record, "ENPC-SB-APPROACH-FAILED", result.message(), false);
            }
            return;
        }
        if (record.phase.filter(value -> value == NpcBackpackTransitionPhase.ALIGNING).isEmpty()) return;
        Optional<NpcNavigationBody> body = state.body(record.actorId);
        if (body.isEmpty()) {
            fail(record, "ENPC-SB-NPC-NOT-MATERIALIZED", "NPC body disappeared during transition.", false);
            return;
        }
        BackpackBlockLocation location = target(record);
        if (!state.alignment.align(body.orElseThrow().mob(), block(location))) return;
        if (record.policy == NpcBackpackTransitionPolicy.ANIMATION_MARKERS) {
            change(record, record.transitionType.orElseThrow() == NpcBackpackTransitionType.EQUIP
                    ? NpcBackpackTransitionPhase.READY_TO_TAKE
                    : NpcBackpackTransitionPhase.ATTACHED_TO_BACK, "NPC aligned; waiting for animation marker.");
        } else {
            immediate(record, body.orElseThrow());
        }
    }

    private void immediate(BackpackActorRecord record, NpcNavigationBody body) {
        try {
            BackpackWorldMutations.MutationResult mutation;
            if (record.transitionType.orElseThrow() == NpcBackpackTransitionType.EQUIP) {
                mutation = state.mutations.extract(record, body.level(), block(record.source.orElseThrow()), state::persist);
                if (mutation.succeeded()) {
                    record.attachmentPoint = NpcBackpackAttachmentPoint.BACK;
                    record.status = NpcBackpackStatus.EQUIPPED;
                }
            } else {
                record.attachmentPoint = NpcBackpackAttachmentPoint.RIGHT_HAND;
                record.revision++;
                state.persist();
                mutation = state.mutations.place(record, body.level(), block(record.destination.orElseThrow()),
                        body.mob().getDirection(), state::persist);
            }
            if (mutation.succeeded()) complete(record, mutation.message());
            else fail(record, mutation.code(), mutation.message(), record.carryingPayload());
        } catch (IOException exception) {
            fail(record, "ENPC-SB-PERSISTENCE-FAILED", exception.getMessage(), true);
        }
    }

    private void complete(BackpackActorRecord record, String message) throws IOException {
        var command = record.commandId.orElseThrow();
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
    }

    private void change(BackpackActorRecord record, NpcBackpackTransitionPhase phase, String message) {
        record.phase = Optional.of(phase);
        record.diagnosticMessage = message;
        record.revision++;
        try {
            state.persist();
        } catch (IOException exception) {
            fail(record, "ENPC-SB-PERSISTENCE-FAILED", exception.getMessage(), true);
        }
    }

    private void fail(BackpackActorRecord record, String code, String message, boolean afterMutation) {
        record.phase = Optional.of(afterMutation ? NpcBackpackTransitionPhase.RECOVERY_REQUIRED
                : NpcBackpackTransitionPhase.FAILED);
        record.status = afterMutation ? NpcBackpackStatus.RECOVERY_REQUIRED
                : record.carryingPayload() ? NpcBackpackStatus.EQUIPPED : NpcBackpackStatus.NONE;
        record.diagnosticCode = code;
        record.diagnosticMessage = message;
        record.commandId.ifPresent(id -> state.terminalCommands.put(id.value(),
                (afterMutation ? NpcBackpackOutcome.RECOVERY_REQUIRED : NpcBackpackOutcome.FAILED).name()));
        record.revision++;
        try { state.persist(); } catch (IOException ignored) { }
    }

    private BackpackBlockLocation target(BackpackActorRecord record) {
        return record.transitionType.orElseThrow() == NpcBackpackTransitionType.EQUIP
                ? record.source.orElseThrow() : record.destination.orElseThrow();
    }

    private BlockPos block(BackpackBlockLocation location) {
        return new BlockPos(location.position().x(), location.position().y(), location.position().z());
    }

    private void trimLedger() {
        while (state.terminalCommands.size() > 1024) {
            state.terminalCommands.remove(state.terminalCommands.keySet().iterator().next());
        }
    }
}
