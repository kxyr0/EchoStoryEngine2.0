package io.github.kxyr0.echonpc.forge.operation;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.form.NpcFormCommandId;
import io.github.kxyr0.echonpc.api.form.NpcFormOutcome;
import io.github.kxyr0.echonpc.api.form.NpcFormStatus;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormChangeResult;
import io.github.kxyr0.echonpc.api.scene.NpcPlaceResult;
import io.github.kxyr0.echonpc.api.scene.NpcPlaceStatus;
import io.github.kxyr0.echonpc.api.scene.NpcSceneCommandId;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.core.actor.ActorRuntime;
import io.github.kxyr0.echonpc.forge.host.ForgeNpcOperationBook;
import java.util.Optional;

/**
 * Maps a caller-supplied mutation ID that is already known by the operation book.
 */
public final class ForgeMutationReuse {
    private final ForgeNpcOperationBook operations;
    private final ActorRuntime runtime;

    ForgeMutationReuse(ForgeNpcOperationBook operations, ActorRuntime runtime) {
        this.operations = java.util.Objects.requireNonNull(operations, "operations");
        this.runtime = java.util.Objects.requireNonNull(runtime, "runtime");
    }

    public Optional<NpcResults.Spawn> spawn(ActorMutationId id, ActorId actorId) {
        return operations.query(id.value()).operation().map(operation -> {
            if (operation.kind() != NpcResults.OperationKind.SPAWN || !operation.actorId().equals(actorId)) {
                return new NpcResults.Spawn(NpcResults.SpawnStatus.REJECTED, operation.actor(), Optional.of(id.value()),
                        "ActorMutationId уже занят другой операцией.");
            }
            return spawnResult(id, actorId, operation);
        });
    }

    public Optional<NpcResults.Teleport> teleport(ActorMutationId id, ActorId actorId) {
        return operations.query(id.value()).operation().map(operation -> {
            if (operation.kind() != NpcResults.OperationKind.TELEPORT || !operation.actorId().equals(actorId)) {
                return new NpcResults.Teleport(NpcResults.TeleportStatus.REJECTED, operation.actor(),
                        Optional.of(id.value()), "ActorMutationId уже занят другой операцией.");
            }
            return teleportResult(id, actorId, operation);
        });
    }

    public Optional<NpcResults.Despawn> despawn(ActorMutationId id, ActorId actorId) {
        return operations.query(id.value()).operation().map(operation -> {
            if (operation.kind() != NpcResults.OperationKind.DESPAWN || !operation.actorId().equals(actorId)) {
                return new NpcResults.Despawn(NpcResults.DespawnStatus.REJECTED, Optional.of(id.value()),
                        "ActorMutationId уже занят другой операцией.");
            }
            return despawnResult(id, operation);
        });
    }

    public Optional<NpcResults.Sleep> sleep(ActorMutationId id, ActorId actorId) {
        return operations.query(id.value()).operation().map(operation -> {
            if (operation.kind() != NpcResults.OperationKind.SLEEP || !operation.actorId().equals(actorId)) {
                return new NpcResults.Sleep(NpcResults.SleepStatus.REJECTED, operation.actor(),
                        Optional.of(id.value()), Optional.empty(),
                        "NPC_SCENE_COMMAND_IDEMPOTENCY_CONFLICT");
            }
            return sleepResult(id, actorId, operation);
        });
    }

    public Optional<NpcResults.Wake> wake(ActorMutationId id, ActorId actorId) {
        return operations.query(id.value()).operation().map(operation -> {
            if (operation.kind() != NpcResults.OperationKind.WAKE || !operation.actorId().equals(actorId)) {
                return new NpcResults.Wake(NpcResults.WakeStatus.REJECTED, operation.actor(),
                        Optional.of(id.value()), "NPC_SCENE_COMMAND_IDEMPOTENCY_CONFLICT");
            }
            return wakeResult(id, actorId, operation);
        });
    }

    public Optional<NpcFormChangeResult> formChange(NpcFormCommandId id, ActorId actorId) {
        return operations.query(id.value()).operation().map(operation -> {
            if (operation.kind() != NpcResults.OperationKind.FORM_CHANGE || !operation.actorId().equals(actorId)) {
                return new NpcFormChangeResult(NpcFormStatus.REJECTED, NpcFormOutcome.OLD_FORM_RETAINED,
                        operation.actor(), Optional.empty(), Optional.of(id.value()),
                        "NPC_FORM_COMMAND_IDEMPOTENCY_CONFLICT");
            }
            return formResult(id, actorId, operation);
        });
    }

    public Optional<NpcPlaceResult> place(NpcSceneCommandId id, ActorId actorId) {
        return operations.query(id.value()).operation().map(operation -> {
            if (operation.kind() != NpcResults.OperationKind.PLACE || !operation.actorId().equals(actorId)) {
                return new NpcPlaceResult(NpcPlaceStatus.REJECTED, operation.actor(), Optional.of(id.value()),
                        "NPC_SCENE_COMMAND_IDEMPOTENCY_CONFLICT");
            }
            return placeResult(id, actorId, operation);
        });
    }

    private NpcResults.Spawn spawnResult(ActorMutationId id, ActorId actorId, NpcResults.Operation operation) {
        return switch (operation.status()) {
            case QUEUED, RUNNING -> new NpcResults.Spawn(NpcResults.SpawnStatus.QUEUED,
                    runtime.query(actorId).actor(), Optional.of(id.value()), "Spawn mutation уже выполняется.");
            case SUCCEEDED -> new NpcResults.Spawn(NpcResults.SpawnStatus.CREATED, operation.actor(),
                    Optional.of(id.value()), "Spawn mutation уже завершена.");
            case REJECTED -> new NpcResults.Spawn(NpcResults.SpawnStatus.REJECTED, operation.actor(),
                    Optional.of(id.value()), operation.message());
            case FAILED -> new NpcResults.Spawn(NpcResults.SpawnStatus.FAILURE, operation.actor(),
                    Optional.of(id.value()), operation.message());
        };
    }

    private NpcResults.Teleport teleportResult(ActorMutationId id, ActorId actorId, NpcResults.Operation operation) {
        return switch (operation.status()) {
            case QUEUED, RUNNING -> new NpcResults.Teleport(NpcResults.TeleportStatus.QUEUED,
                    runtime.query(actorId).actor(), Optional.of(id.value()), "Teleport mutation уже выполняется.");
            case SUCCEEDED -> new NpcResults.Teleport(NpcResults.TeleportStatus.MOVED, operation.actor(),
                    Optional.of(id.value()), "Teleport mutation уже завершена.");
            case REJECTED -> new NpcResults.Teleport(NpcResults.TeleportStatus.REJECTED, operation.actor(),
                    Optional.of(id.value()), operation.message());
            case FAILED -> new NpcResults.Teleport(NpcResults.TeleportStatus.FAILURE, operation.actor(),
                    Optional.of(id.value()), operation.message());
        };
    }

    private NpcResults.Despawn despawnResult(ActorMutationId id, NpcResults.Operation operation) {
        return switch (operation.status()) {
            case QUEUED, RUNNING -> new NpcResults.Despawn(NpcResults.DespawnStatus.QUEUED,
                    Optional.of(id.value()), "Despawn mutation уже выполняется.");
            case SUCCEEDED -> new NpcResults.Despawn(NpcResults.DespawnStatus.REMOVED,
                    Optional.of(id.value()), "Despawn mutation уже завершена.");
            case REJECTED -> new NpcResults.Despawn(NpcResults.DespawnStatus.REJECTED,
                    Optional.of(id.value()), operation.message());
            case FAILED -> new NpcResults.Despawn(NpcResults.DespawnStatus.FAILURE,
                    Optional.of(id.value()), operation.message());
        };
    }

    private NpcResults.Sleep sleepResult(ActorMutationId id, ActorId actorId, NpcResults.Operation operation) {
        var actor = runtime.query(actorId).actor();
        var attachment = actor.flatMap(io.github.kxyr0.echonpc.api.service.NpcActorView::restAttachment);
        return switch (operation.status()) {
            case QUEUED, RUNNING -> new NpcResults.Sleep(NpcResults.SleepStatus.QUEUED, actor,
                    Optional.of(id.value()), attachment, "SleepAt mutation уже выполняется.");
            case SUCCEEDED -> new NpcResults.Sleep(NpcResults.SleepStatus.SLEEPING, actor,
                    Optional.of(id.value()), attachment, "SleepAt mutation уже завершена.");
            case REJECTED -> new NpcResults.Sleep(NpcResults.SleepStatus.REJECTED, actor,
                    Optional.of(id.value()), attachment, operation.message());
            case FAILED -> new NpcResults.Sleep(NpcResults.SleepStatus.FAILURE, actor,
                    Optional.of(id.value()), attachment, operation.message());
        };
    }

    private NpcResults.Wake wakeResult(ActorMutationId id, ActorId actorId, NpcResults.Operation operation) {
        return switch (operation.status()) {
            case QUEUED, RUNNING -> new NpcResults.Wake(NpcResults.WakeStatus.QUEUED,
                    runtime.query(actorId).actor(), Optional.of(id.value()), "WakeUp mutation уже выполняется.");
            case SUCCEEDED -> new NpcResults.Wake(NpcResults.WakeStatus.WOKE_UP, operation.actor(),
                    Optional.of(id.value()), "WakeUp mutation уже завершена.");
            case REJECTED -> new NpcResults.Wake(NpcResults.WakeStatus.REJECTED, operation.actor(),
                    Optional.of(id.value()), operation.message());
            case FAILED -> new NpcResults.Wake(NpcResults.WakeStatus.FAILURE, operation.actor(),
                    Optional.of(id.value()), operation.message());
        };
    }

    private NpcFormChangeResult formResult(NpcFormCommandId id, ActorId actorId,
            NpcResults.Operation operation) {
        return switch (operation.status()) {
            case QUEUED, RUNNING -> new NpcFormChangeResult(NpcFormStatus.QUEUED,
                    NpcFormOutcome.UNCHANGED, runtime.query(actorId).actor(), Optional.empty(),
                    Optional.of(id.value()), "Npc.SetForm mutation уже выполняется.");
            case SUCCEEDED -> new NpcFormChangeResult(NpcFormStatus.COMMITTED,
                    NpcFormOutcome.ACTIVE_FORM_CHANGED, operation.actor(), Optional.empty(),
                    Optional.of(id.value()), "Npc.SetForm mutation уже завершена.");
            case REJECTED -> new NpcFormChangeResult(NpcFormStatus.REJECTED,
                    NpcFormOutcome.OLD_FORM_RETAINED, operation.actor(), Optional.empty(),
                    Optional.of(id.value()), operation.message());
            case FAILED -> new NpcFormChangeResult(NpcFormStatus.FAILURE,
                    NpcFormOutcome.OLD_FORM_RETAINED, operation.actor(), Optional.empty(),
                    Optional.of(id.value()), operation.message());
        };
    }

    private NpcPlaceResult placeResult(NpcSceneCommandId id, ActorId actorId, NpcResults.Operation operation) {
        return switch (operation.status()) {
            case QUEUED, RUNNING -> new NpcPlaceResult(NpcPlaceStatus.QUEUED,
                    runtime.query(actorId).actor(), Optional.of(id.value()), "Npc.PlaceAt уже выполняется.");
            case SUCCEEDED -> new NpcPlaceResult(NpcPlaceStatus.PLACED,
                    operation.actor(), Optional.of(id.value()), "Npc.PlaceAt уже завершена.");
            case REJECTED -> new NpcPlaceResult(NpcPlaceStatus.REJECTED,
                    operation.actor(), Optional.of(id.value()), operation.message());
            case FAILED -> new NpcPlaceResult(NpcPlaceStatus.FAILURE,
                    operation.actor(), Optional.of(id.value()), operation.message());
        };
    }
}
