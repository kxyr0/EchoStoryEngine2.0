package io.github.kxyr0.echonpcbackpack.forge.runtime;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBody;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackAttachmentId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackReservationId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackTransitionId;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackCommandResult;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackEquipRequest;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackUnequipRequest;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackOutcome;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackStatus;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPhase;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionType;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import io.github.kxyr0.echonpcbackpack.forge.approach.BackpackApproachCoordinator;
import io.github.kxyr0.echonpcbackpack.forge.identity.BackpackForgeIds;
import io.github.kxyr0.echonpcbackpack.forge.sophisticated.BackpackPayloadSnapshot;
import io.github.kxyr0.echonpcbackpack.forge.state.BackpackActorRecord;
import java.io.IOException;
import java.util.Optional;

public final class BackpackCommandCoordinator {
    private final BackpackRuntimeState state;

    public BackpackCommandCoordinator(BackpackRuntimeState state) {
        this.state = state;
    }

    public NpcBackpackCommandResult equip(NpcBackpackEquipRequest request) {
        NpcBackpackCommandResult replay = replay(request.actorId(), request.commandId());
        if (replay != null) return replay;
        BackpackActorRecord current = state.actors.get(request.actorId().value());
        if (current != null && current.transitioning()) return rejected(request.commandId(), current,
                "ENPC-SB-TRANSITION-BUSY", "NPC already has an active backpack transition.");
        if (current != null && current.carryingPayload()) return terminal(request.commandId(), current,
                NpcBackpackOutcome.ALREADY_SATISFIED, "ENPC-SB-ALREADY-EQUIPPED", "NPC already wears a backpack.");
        if (state.reservedByOther(request.actorId(), request.source())) return rejected(request.commandId(), current,
                "ENPC-SB-RESERVED", "Backpack source is reserved by another NPC.");
        var level = state.level(request.source());
        if (level.isEmpty()) return rejected(request.commandId(), current, "ENPC-SB-WORLD-MISSING",
                "Source dimension is unavailable.");
        Optional<BackpackPayloadSnapshot> payload = state.access.inspect(level.orElseThrow(), block(request.source()));
        if (payload.isEmpty()) return rejected(request.commandId(), current, "ENPC-SB-NOT-BACKPACK",
                "Source is not a valid Sophisticated Backpack block.");
        if (state.payloadOwnedByOther(request.actorId(), payload.orElseThrow().reference().contentsUuid())) {
            return rejected(request.commandId(), current, "ENPC-SB-PAYLOAD-OWNED",
                    "This exact backpack UUID already belongs to another NPC.");
        }
        return begin(request.actorId(), request.source(), request.commandId(), request.policy(),
                NpcBackpackTransitionType.EQUIP, payload.orElseThrow().reference().fingerprint());
    }

    public NpcBackpackCommandResult unequip(NpcBackpackUnequipRequest request) {
        NpcBackpackCommandResult replay = replay(request.actorId(), request.commandId());
        if (replay != null) return replay;
        BackpackActorRecord current = state.actors.get(request.actorId().value());
        if (current != null && current.transitioning()) return rejected(request.commandId(), current,
                "ENPC-SB-TRANSITION-BUSY", "NPC already has an active backpack transition.");
        if (current == null || !current.carryingPayload()) return terminal(request.commandId(), current,
                NpcBackpackOutcome.ALREADY_SATISFIED, "ENPC-SB-NOT-EQUIPPED", "NPC does not wear a backpack.");
        if (state.reservedByOther(request.actorId(), request.destination())) return rejected(request.commandId(), current,
                "ENPC-SB-RESERVED", "Destination is reserved by another NPC.");
        var level = state.level(request.destination());
        if (level.isEmpty()) return rejected(request.commandId(), current, "ENPC-SB-WORLD-MISSING",
                "Destination dimension is unavailable.");
        if (!level.orElseThrow().getBlockState(block(request.destination())).canBeReplaced()
                && !state.access.exactAt(level.orElseThrow(), block(request.destination()), current.payload)) {
            return rejected(request.commandId(), current, "ENPC-SB-DESTINATION-OCCUPIED", "Destination is occupied.");
        }
        return begin(request.actorId(), request.destination(), request.commandId(), request.policy(),
                NpcBackpackTransitionType.UNEQUIP, current.expectedFingerprint.orElse("unknown"));
    }

    private NpcBackpackCommandResult begin(ActorId actor, BackpackBlockLocation location,
            NpcBackpackCommandId commandId, io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPolicy policy,
            NpcBackpackTransitionType type, String fingerprint) {
        Optional<NpcNavigationBody> body = state.body(actor);
        if (body.isEmpty()) return rejected(commandId, state.actors.get(actor.value()), "ENPC-SB-NPC-NOT-MATERIALIZED",
                "NPC body is not materialized.");
        if (!body.orElseThrow().dimensionId().equals(location.dimension().value())) {
            return rejected(commandId, state.actors.get(actor.value()), "ENPC-SB-WORLD-MISMATCH",
                    "NPC and backpack location are in different dimensions.");
        }
        String suffix = BackpackForgeIds.hash(commandId.value());
        BackpackActorRecord record = state.actors.computeIfAbsent(actor.value(), ignored -> newRecord(actor));
        record.transitionId = Optional.of(new NpcBackpackTransitionId("echo_npc_sophisticated_backpacks:" + suffix));
        record.commandId = Optional.of(commandId);
        record.reservationId = Optional.of(new NpcBackpackReservationId("echo_npc_sophisticated_backpacks:" + suffix));
        record.transitionType = Optional.of(type);
        record.phase = Optional.of(NpcBackpackTransitionPhase.RESERVED);
        record.policy = policy;
        record.source = type == NpcBackpackTransitionType.EQUIP ? Optional.of(location) : Optional.empty();
        record.destination = type == NpcBackpackTransitionType.UNEQUIP ? Optional.of(location) : Optional.empty();
        record.expectedFingerprint = Optional.of(fingerprint);
        record.status = NpcBackpackStatus.TRANSITIONING;
        record.diagnosticCode = "ENPC-SB-OK";
        record.diagnosticMessage = "Transition accepted.";
        record.revision++;
        if (!persist(record, "Could not persist reservation.")) return failure(commandId, record);
        BackpackApproachCoordinator.StartResult approach = state.approach.start(
                state.server(), body.orElseThrow(), location, commandId);
        if (approach.status() == BackpackApproachCoordinator.Status.MOVING) {
            record.phase = Optional.of(NpcBackpackTransitionPhase.APPROACHING);
            record.navigationMoveId = approach.moveId();
        } else if (approach.status() == BackpackApproachCoordinator.Status.ARRIVED) {
            record.phase = Optional.of(NpcBackpackTransitionPhase.ALIGNING);
        } else {
            return failBeforeMutation(commandId, record, "ENPC-SB-APPROACH-FAILED", approach.message());
        }
        record.revision++;
        if (!persist(record, "Could not persist navigation state.")) return failure(commandId, record);
        return result(NpcBackpackOutcome.ACCEPTED, commandId, record, "ENPC-SB-ACCEPTED", "Transition accepted.");
    }

    private BackpackActorRecord newRecord(ActorId actor) {
        BackpackActorRecord record = new BackpackActorRecord();
        record.actorId = actor;
        record.attachmentId = new NpcBackpackAttachmentId("echo_npc_sophisticated_backpacks:"
                + BackpackForgeIds.hash(actor.value()));
        return record;
    }

    private NpcBackpackCommandResult replay(ActorId actor, NpcBackpackCommandId commandId) {
        BackpackActorRecord record = state.actors.get(actor.value());
        if (record != null && record.commandId.filter(commandId::equals).isPresent()) {
            return result(NpcBackpackOutcome.ACCEPTED, commandId, record, "ENPC-SB-DUPLICATE", "Command is active.");
        }
        String outcome = state.terminalCommands.get(commandId.value());
        return outcome == null ? null : result(NpcBackpackOutcome.valueOf(outcome), commandId, record,
                "ENPC-SB-DUPLICATE", "Terminal command result replayed.");
    }

    private NpcBackpackCommandResult failBeforeMutation(NpcBackpackCommandId id, BackpackActorRecord record,
            String code, String message) {
        record.status = record.carryingPayload() ? NpcBackpackStatus.EQUIPPED : NpcBackpackStatus.NONE;
        record.diagnosticCode = code;
        record.diagnosticMessage = message;
        record.phase = Optional.of(NpcBackpackTransitionPhase.FAILED);
        record.revision++;
        state.terminalCommands.put(id.value(), NpcBackpackOutcome.FAILED.name());
        record.clearTransition();
        persist(record, "Could not persist failure.");
        return result(NpcBackpackOutcome.FAILED, id, record, code, message);
    }

    private NpcBackpackCommandResult rejected(NpcBackpackCommandId id, BackpackActorRecord record,
            String code, String message) {
        state.terminalCommands.put(id.value(), NpcBackpackOutcome.REJECTED.name());
        try { state.persist(); } catch (IOException ignored) { }
        return result(NpcBackpackOutcome.REJECTED, id, record, code, message);
    }

    private NpcBackpackCommandResult terminal(NpcBackpackCommandId id, BackpackActorRecord record,
            NpcBackpackOutcome outcome, String code, String message) {
        state.terminalCommands.put(id.value(), outcome.name());
        try { state.persist(); } catch (IOException ignored) { }
        return result(outcome, id, record, code, message);
    }

    private boolean persist(BackpackActorRecord record, String message) {
        try {
            state.persist();
            return true;
        } catch (IOException exception) {
            record.status = NpcBackpackStatus.RECOVERY_REQUIRED;
            record.diagnosticCode = "ENPC-SB-PERSISTENCE-FAILED";
            record.diagnosticMessage = message + " " + exception.getMessage();
            return false;
        }
    }

    private NpcBackpackCommandResult failure(NpcBackpackCommandId id, BackpackActorRecord record) {
        return result(NpcBackpackOutcome.RECOVERY_REQUIRED, id, record,
                record.diagnosticCode, record.diagnosticMessage);
    }

    private NpcBackpackCommandResult result(NpcBackpackOutcome outcome, NpcBackpackCommandId id,
            BackpackActorRecord record, String code, String message) {
        return new NpcBackpackCommandResult(outcome, id,
                record == null ? Optional.empty() : Optional.of(state.views.view(record)), code, message);
    }

    private net.minecraft.core.BlockPos block(BackpackBlockLocation location) {
        return new net.minecraft.core.BlockPos(location.position().x(), location.position().y(), location.position().z());
    }

}
