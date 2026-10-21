package io.github.kxyr0.echonpcbackpack.forge.runtime;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackCommandResult;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackAttachmentPoint;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackOutcome;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackStatus;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPhase;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionType;
import io.github.kxyr0.echonpcbackpack.forge.sophisticated.SophisticatedBackpackAccess;
import io.github.kxyr0.echonpcbackpack.forge.state.BackpackActorRecord;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.core.BlockPos;

public final class BackpackRecoveryCoordinator {
    private final BackpackRuntimeState state;

    public BackpackRecoveryCoordinator(BackpackRuntimeState state) {
        this.state = state;
    }

    public void recoverAll() {
        state.actors.values().stream().filter(record -> record.transitioning()
                || record.status == NpcBackpackStatus.RECOVERY_REQUIRED).toList().forEach(this::recoverSafely);
    }

    public NpcBackpackCommandResult retry(ActorId actor, NpcBackpackCommandId commandId) {
        BackpackActorRecord record = state.actors.get(actor.value());
        if (record == null) return result(NpcBackpackOutcome.REJECTED, commandId, null,
                "ENPC-SB-NPC-UNKNOWN", "NPC has no backpack state.");
        try {
            boolean recovered = recover(record);
            return result(recovered ? NpcBackpackOutcome.SUCCEEDED : NpcBackpackOutcome.RECOVERY_REQUIRED,
                    commandId, record, record.diagnosticCode, record.diagnosticMessage);
        } catch (IOException exception) {
            block(record, "ENPC-SB-PERSISTENCE-FAILED", exception.getMessage());
            return result(NpcBackpackOutcome.RECOVERY_REQUIRED, commandId, record,
                    record.diagnosticCode, record.diagnosticMessage);
        }
    }

    private void recoverSafely(BackpackActorRecord record) {
        try {
            recover(record);
        } catch (IOException exception) {
            block(record, "ENPC-SB-PERSISTENCE-FAILED", exception.getMessage());
        }
    }

    private boolean recover(BackpackActorRecord record) throws IOException {
        if (record.transitionType.isEmpty()) {
            if (record.carryingPayload()) {
                record.status = NpcBackpackStatus.EQUIPPED;
                record.attachmentPoint = NpcBackpackAttachmentPoint.BACK;
                record.diagnosticCode = "ENPC-SB-OK";
                record.diagnosticMessage = "Stable equipped payload recovered.";
                state.persist();
                return true;
            }
            record.status = NpcBackpackStatus.NONE;
            return true;
        }
        return record.transitionType.orElseThrow() == NpcBackpackTransitionType.EQUIP
                ? recoverEquip(record) : recoverUnequip(record);
    }

    private boolean recoverEquip(BackpackActorRecord record) throws IOException {
        var level = record.source.flatMap(state::level);
        if (level.isEmpty()) return block(record, "ENPC-SB-RECOVERY-WORLD-MISSING", "Source world is unavailable.");
        Optional<io.github.kxyr0.echonpcbackpack.forge.sophisticated.BackpackPayloadSnapshot> source =
                state.access.inspect(level.orElseThrow(), block(record.source.orElseThrow()));
        if (!record.carryingPayload()) {
            if (source.isPresent()) return failWithoutPayload(record, "Source still exists; no destructive step occurred.");
            return block(record, "ENPC-SB-RECOVERY-PAYLOAD-MISSING", "Source and persisted payload are both absent.");
        }
        if (source.isPresent()) {
            String expected = record.expectedFingerprint.orElse("");
            if (!source.orElseThrow().reference().fingerprint().equals(expected)) {
                return block(record, "ENPC-SB-RECOVERY-DUPLICATE-CONFLICT",
                        "Source and payload both exist but do not match.");
            }
            var removal = state.access.removeExact(level.orElseThrow(), block(record.source.orElseThrow()), expected);
            if (removal.status() != SophisticatedBackpackAccess.Status.REMOVED) {
                return block(record, "ENPC-SB-RECOVERY-REMOVE-FAILED", "Could not remove duplicate source.");
            }
        }
        record.attachmentPoint = NpcBackpackAttachmentPoint.BACK;
        record.status = NpcBackpackStatus.EQUIPPED;
        return finish(record, "Recovered persisted payload onto NPC back.");
    }

    private boolean recoverUnequip(BackpackActorRecord record) throws IOException {
        var level = record.destination.flatMap(state::level);
        if (level.isEmpty()) return block(record, "ENPC-SB-RECOVERY-WORLD-MISSING", "Destination world is unavailable.");
        BlockPos destination = block(record.destination.orElseThrow());
        boolean expectedAtDestination = record.carryingPayload()
                ? state.access.exactAt(level.orElseThrow(), destination, record.payload)
                : state.access.inspect(level.orElseThrow(), destination)
                .filter(payload -> record.expectedFingerprint.filter(
                        payload.reference().fingerprint()::equals).isPresent()).isPresent();
        if (expectedAtDestination) {
            record.payload = net.minecraft.world.item.ItemStack.EMPTY;
            record.attachmentPoint = NpcBackpackAttachmentPoint.NONE;
            record.status = NpcBackpackStatus.NONE;
            return finish(record, "Verified existing destination backpack and completed recovery.");
        }
        if (!level.orElseThrow().getBlockState(destination).canBeReplaced()) {
            record.attachmentPoint = NpcBackpackAttachmentPoint.BACK;
            return block(record, "ENPC-SB-RECOVERY-DESTINATION-OCCUPIED",
                    "Destination is occupied; payload remains on NPC back.");
        }
        record.attachmentPoint = NpcBackpackAttachmentPoint.BACK;
        return block(record, "ENPC-SB-RECOVERY-RETRY-UNEQUIP",
                "Payload is safe on NPC; repeat unequip to an empty destination.");
    }

    private boolean failWithoutPayload(BackpackActorRecord record, String message) throws IOException {
        record.status = NpcBackpackStatus.NONE;
        record.phase = Optional.of(NpcBackpackTransitionPhase.FAILED);
        record.diagnosticCode = "ENPC-SB-RECOVERY-SOURCE-UNCHANGED";
        record.diagnosticMessage = message;
        record.clearTransition();
        record.revision++;
        state.persist();
        return true;
    }

    private boolean finish(BackpackActorRecord record, String message) throws IOException {
        record.commandId.ifPresent(id -> state.terminalCommands.put(id.value(), NpcBackpackOutcome.SUCCEEDED.name()));
        record.phase = Optional.of(NpcBackpackTransitionPhase.COMPLETED);
        record.diagnosticCode = "ENPC-SB-OK";
        record.diagnosticMessage = message;
        record.clearTransition();
        if (!record.carryingPayload()) record.expectedFingerprint = Optional.empty();
        record.revision++;
        state.persist();
        return true;
    }

    private boolean block(BackpackActorRecord record, String code, String message) {
        record.status = NpcBackpackStatus.RECOVERY_REQUIRED;
        record.phase = Optional.of(NpcBackpackTransitionPhase.RECOVERY_REQUIRED);
        record.diagnosticCode = code;
        record.diagnosticMessage = message;
        record.revision++;
        try { state.persist(); } catch (IOException ignored) { }
        return false;
    }

    private BlockPos block(io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation location) {
        return new BlockPos(location.position().x(), location.position().y(), location.position().z());
    }

    private NpcBackpackCommandResult result(NpcBackpackOutcome outcome, NpcBackpackCommandId command,
            BackpackActorRecord record, String code, String message) {
        return new NpcBackpackCommandResult(outcome, command,
                record == null ? Optional.empty() : Optional.of(state.views.view(record)), code, message);
    }
}
