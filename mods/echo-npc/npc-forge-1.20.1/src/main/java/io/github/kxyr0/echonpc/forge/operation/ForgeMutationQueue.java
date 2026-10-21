package io.github.kxyr0.echonpc.forge.operation;

import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeRequest;
import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeResult;
import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeStatus;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.form.NpcFormOutcome;
import io.github.kxyr0.echonpc.api.form.NpcFormStatus;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormChangeResult;
import io.github.kxyr0.echonpc.api.scene.NpcPlaceResult;
import io.github.kxyr0.echonpc.api.scene.NpcPlaceStatus;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.core.actor.ActorRuntime;
import io.github.kxyr0.echonpc.core.diagnostic.DiagnosticSink;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.forge.host.ForgeNpcOperationBook;
import io.github.kxyr0.echonpc.forge.persistence.PersistenceWorkQueue;
import java.util.Map;
import java.util.Optional;

/**
 * Submits durable actor mutation work and records the public operation status.
 */
public final class ForgeMutationQueue {
    private final ActorRuntime runtime;
    private final PersistenceWorkQueue queue;
    private final ForgeNpcOperationBook operations;
    private final DiagnosticSink diagnostics;
    private final Map<String, ForgePendingMutation> pending;

    ForgeMutationQueue(ActorRuntime runtime, PersistenceWorkQueue queue, ForgeNpcOperationBook operations,
            DiagnosticSink diagnostics, Map<String, ForgePendingMutation> pending) {
        this.runtime = java.util.Objects.requireNonNull(runtime, "runtime");
        this.queue = java.util.Objects.requireNonNull(queue, "queue");
        this.operations = java.util.Objects.requireNonNull(operations, "operations");
        this.diagnostics = java.util.Objects.requireNonNull(diagnostics, "diagnostics");
        this.pending = java.util.Objects.requireNonNull(pending, "pending");
    }

    public String submit(ForgePendingMutation mutation, Optional<ActorMutationId> mutationId,
            java.util.concurrent.Callable<Object> work) {
        String id = mutationId.map(ActorMutationId::value).orElseGet(operations::nextId);
        return submit(id, mutation, work);
    }

    public String submit(String id, ForgePendingMutation mutation, java.util.concurrent.Callable<Object> work) {
        var desired = mutation.desired();
        pending.put(id, mutation);
        operations.queued(id, mutation.kind(), desired.actorId(),
                "Операция поставлена в persistence queue.");
        if (!queue.submit(id, work)) {
            pending.remove(id);
            operations.rejected(id, mutation.kind(), desired.actorId(), "Persistence queue заполнена.");
            diagnostics.accept(NpcDiagnostic.warning("ENPC-PERSISTENCE-QUEUE-FULL",
                    "Persistence queue saturated.", desired.actorId().value()));
        }
        return id;
    }

    public NpcResults.Spawn queuedSpawn(ActorId actorId, String operationId) {
        var query = operations.query(operationId);
        if (query.operation().orElseThrow().status() == NpcResults.OperationStatus.REJECTED) {
            return new NpcResults.Spawn(NpcResults.SpawnStatus.REJECTED, Optional.empty(),
                    Optional.of(operationId), "Spawn отклонён: очередь persistence заполнена.");
        }
        return new NpcResults.Spawn(NpcResults.SpawnStatus.QUEUED, runtime.query(actorId).actor(),
                Optional.of(operationId), "Spawn сохранится перед materialization.");
    }

    public NpcResults.Sleep queuedSleep(ActorId actorId, String operationId) {
        var query = operations.query(operationId);
        if (query.operation().orElseThrow().status() == NpcResults.OperationStatus.REJECTED) {
            return new NpcResults.Sleep(NpcResults.SleepStatus.REJECTED, runtime.query(actorId).actor(),
                    Optional.of(operationId), Optional.empty(), "SleepAt отклонён: очередь persistence заполнена.");
        }
        return new NpcResults.Sleep(NpcResults.SleepStatus.QUEUED, runtime.query(actorId).actor(),
                Optional.of(operationId), Optional.empty(), "SleepAt сохранится перед attachment dispatch.");
    }

    public NpcResults.Wake queuedWake(ActorId actorId, String operationId) {
        var query = operations.query(operationId);
        if (query.operation().orElseThrow().status() == NpcResults.OperationStatus.REJECTED) {
            return new NpcResults.Wake(NpcResults.WakeStatus.REJECTED, runtime.query(actorId).actor(),
                    Optional.of(operationId), "WakeUp отклонён: очередь persistence заполнена.");
        }
        return new NpcResults.Wake(NpcResults.WakeStatus.QUEUED, runtime.query(actorId).actor(),
                Optional.of(operationId), "WakeUp сохранится перед standing transform.");
    }

    public NpcFormChangeResult queuedFormChange(ActorId actorId, String operationId) {
        var query = operations.query(operationId);
        if (query.operation().orElseThrow().status() == NpcResults.OperationStatus.REJECTED) {
            return new NpcFormChangeResult(NpcFormStatus.REJECTED, NpcFormOutcome.OLD_FORM_RETAINED,
                    runtime.query(actorId).actor(), Optional.empty(), Optional.of(operationId),
                    "Npc.SetForm отклонён: очередь persistence заполнена.");
        }
        return new NpcFormChangeResult(NpcFormStatus.QUEUED, NpcFormOutcome.UNCHANGED,
                runtime.query(actorId).actor(), Optional.empty(), Optional.of(operationId),
                "Npc.SetForm сохранится перед atomic publication.");
    }

    public NpcSkinChangeResult queuedSkinChange(NpcSkinChangeRequest request, String operationId) {
        var query = operations.query(operationId);
        if (query.operation().orElseThrow().status() == NpcResults.OperationStatus.REJECTED) {
            return new NpcSkinChangeResult(NpcSkinChangeStatus.REJECTED, request.skinId(),
                    runtime.query(request.npcId()).actor(), Optional.of(operationId),
                    "Npc.ChangeSkin отклонён: очередь persistence заполнена.");
        }
        return new NpcSkinChangeResult(NpcSkinChangeStatus.QUEUED, request.skinId(),
                runtime.query(request.npcId()).actor(), Optional.of(operationId),
                "Npc.ChangeSkin сохранится перед atomic publication.");
    }

    public NpcPlaceResult queuedPlace(ActorId actorId, String operationId) {
        var query = operations.query(operationId);
        if (query.operation().orElseThrow().status() == NpcResults.OperationStatus.REJECTED) {
            return new NpcPlaceResult(NpcPlaceStatus.REJECTED, runtime.query(actorId).actor(),
                    Optional.of(operationId), "Npc.PlaceAt отклонён: очередь persistence заполнена.");
        }
        return new NpcPlaceResult(NpcPlaceStatus.QUEUED, runtime.query(actorId).actor(),
                Optional.of(operationId), "Npc.PlaceAt сохранится перед перемещением.");
    }
}
