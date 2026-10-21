package io.github.kxyr0.echonpc.forge.operation;

import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeRequest;
import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeResult;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormChangeRequest;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormChangeResult;
import io.github.kxyr0.echonpc.api.rest.command.NpcSleepRequest;
import io.github.kxyr0.echonpc.api.rest.command.NpcWakeRequest;
import io.github.kxyr0.echonpc.api.scene.NpcPlaceRequest;
import io.github.kxyr0.echonpc.api.scene.NpcPlaceResult;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.SpawnNpcRequest;
import io.github.kxyr0.echonpc.api.service.TeleportNpcRequest;
import io.github.kxyr0.echonpc.core.actor.ActorRuntime;
import io.github.kxyr0.echonpc.core.definition.NpcCatalogState;
import io.github.kxyr0.echonpc.core.diagnostic.DiagnosticSink;
import io.github.kxyr0.echonpc.core.persistence.ActorStateRepository;
import io.github.kxyr0.echonpc.forge.host.ForgeActorMaterializer;
import io.github.kxyr0.echonpc.forge.host.ForgeNpcOperationBook;
import io.github.kxyr0.echonpc.forge.persistence.PersistenceWorkQueue;
import io.github.kxyr0.echonpc.forge.recovery.ForgeNpcRecoveryFlow;
import io.github.kxyr0.echonpc.forge.rest.ForgeRestRuntime;
import io.github.kxyr0.echonpc.forge.spawn.NpcSpawnPositionValidator;
import io.github.kxyr0.echonpc.forge.operation.actor.ForgeNpcActorOperations;
import io.github.kxyr0.echonpc.forge.operation.form.ForgeNpcFormOperations;
import io.github.kxyr0.echonpc.forge.operation.place.ForgeNpcPlaceOperations;
import io.github.kxyr0.echonpc.forge.operation.skin.ForgeNpcSkinOperations;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Public mutating service surface backed by durable queued persistence.
 */
public final class ForgeNpcMutationService {
    private final ForgeNpcOperationBook operations;
    private final ForgeNpcRecoveryFlow recovery;
    private final ForgeNpcMutationFlow mutations;
    private final ForgeNpcActorOperations actorOperations;
    private final ForgeNpcRestOperations restOperations;
    private final ForgeNpcFormOperations formOperations;
    private final ForgeNpcSkinOperations skinOperations;
    private final ForgeNpcPlaceOperations placeOperations;
    private final Map<String, ForgePendingMutation> pending = new HashMap<>();

    public ForgeNpcMutationService(BooleanSupplier ready, NpcCatalogState catalogState, ActorRuntime runtime,
            ForgeActorMaterializer materializer, ActorStateRepository repository, PersistenceWorkQueue queue,
            ForgeNpcOperationBook operations, ForgeNpcRecoveryFlow recovery, ForgeNpcMutationFlow mutations,
            ForgeRestRuntime restRuntime, NpcSpawnPositionValidator spawnPositionValidator,
            DiagnosticSink diagnostics, Clock clock) {
        this.operations = java.util.Objects.requireNonNull(operations, "operations");
        this.recovery = java.util.Objects.requireNonNull(recovery, "recovery");
        this.mutations = java.util.Objects.requireNonNull(mutations, "mutations");
        ForgeMutationReuse reuse = new ForgeMutationReuse(operations, runtime);
        ForgeMutationQueue submission = new ForgeMutationQueue(runtime, queue, operations, diagnostics, pending);
        NpcSpawnPositionValidator positionValidator = java.util.Objects.requireNonNull(spawnPositionValidator,
                "spawnPositionValidator");
        ForgeSpawnMutationRules spawnRules = new ForgeSpawnMutationRules(runtime, clock);
        this.actorOperations = new ForgeNpcActorOperations(ready, catalogState, runtime, materializer, repository,
                reuse, submission, spawnRules, positionValidator, diagnostics, clock);
        this.restOperations = new ForgeNpcRestOperations(ready, runtime, materializer, repository, operations,
                reuse, submission, restRuntime, diagnostics, clock);
        this.formOperations = new ForgeNpcFormOperations(ready, catalogState, runtime, materializer, repository,
                reuse, submission, diagnostics, clock);
        this.skinOperations = new ForgeNpcSkinOperations(ready, runtime, materializer, repository, operations,
                submission, clock);
        this.placeOperations = new ForgeNpcPlaceOperations(ready, runtime, materializer, repository, reuse,
                submission, formOperations, positionValidator, diagnostics, clock);
    }

    public NpcResults.Spawn spawn(SpawnNpcRequest request) { return actorOperations.spawn(request); }

    public NpcResults.Teleport teleport(TeleportNpcRequest request) { return actorOperations.teleport(request); }

    public NpcResults.Despawn despawn(ActorId actorId) { return actorOperations.despawn(actorId, Optional.empty()); }

    public NpcResults.Despawn despawn(ActorId actorId, ActorMutationId mutationId) {
        return actorOperations.despawn(actorId, Optional.of(java.util.Objects.requireNonNull(mutationId,
                "mutationId")));
    }

    public NpcResults.Sleep sleepAt(NpcSleepRequest request) { return restOperations.sleepAt(request); }

    public NpcResults.Wake wakeUp(NpcWakeRequest request) { return restOperations.wakeUp(request); }

    public NpcFormChangeResult setForm(NpcFormChangeRequest request) { return formOperations.setForm(request); }

    public NpcSkinChangeResult changeSkin(NpcSkinChangeRequest request) { return skinOperations.changeSkin(request); }

    public NpcPlaceResult placeAt(NpcPlaceRequest request) { return placeOperations.placeAt(request); }

    public NpcResults.Spawn retryRecovery(ActorId actorId) { return recovery.retry(actorId); }

    public boolean applyCompleted(PersistenceWorkQueue.Completed completed) {
        ForgePendingMutation mutation = pending.remove(completed.operationId());
        if (mutation == null) {
            return false;
        }
        if (completed.failed()) {
            operations.failed(completed.operationId(), Optional.empty(),
                    completed.failure().getClass().getSimpleName());
            return true;
        }
        return mutations.apply(completed.operationId(), mutation, completed.result());
    }

    public void clear() { pending.clear(); }
}
