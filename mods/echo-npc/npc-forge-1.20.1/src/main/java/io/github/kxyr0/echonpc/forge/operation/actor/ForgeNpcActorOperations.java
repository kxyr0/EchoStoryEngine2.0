package io.github.kxyr0.echonpc.forge.operation.actor;

import io.github.kxyr0.echonpc.api.form.definition.NpcFormDefinition;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.SpawnNpcRequest;
import io.github.kxyr0.echonpc.api.service.TeleportNpcRequest;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.actor.ActorRuntime;
import io.github.kxyr0.echonpc.core.actor.ActiveActor;
import io.github.kxyr0.echonpc.core.definition.NpcCatalogState;
import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import io.github.kxyr0.echonpc.core.diagnostic.DiagnosticSink;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.core.persistence.ActorStateRepository;
import io.github.kxyr0.echonpc.forge.host.ForgeActorMaterializer;
import io.github.kxyr0.echonpc.forge.operation.ForgeMutationQueue;
import io.github.kxyr0.echonpc.forge.operation.ForgeMutationReuse;
import io.github.kxyr0.echonpc.forge.operation.ForgePendingMutation;
import io.github.kxyr0.echonpc.forge.operation.ForgeSpawnMutationRules;
import io.github.kxyr0.echonpc.forge.spawn.NpcSpawnPositionValidator;
import java.time.Clock;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Durable server-side actor lifecycle operations.
 */
public final class ForgeNpcActorOperations {
    private final BooleanSupplier ready;
    private final NpcCatalogState catalogState;
    private final ActorRuntime runtime;
    private final ForgeActorMaterializer materializer;
    private final ActorStateRepository repository;
    private final ForgeMutationReuse reuse;
    private final ForgeMutationQueue submission;
    private final ForgeSpawnMutationRules spawnRules;
    private final NpcSpawnPositionValidator positionValidator;
    private final DiagnosticSink diagnostics;
    private final Clock clock;

    public ForgeNpcActorOperations(BooleanSupplier ready, NpcCatalogState catalogState, ActorRuntime runtime,
            ForgeActorMaterializer materializer, ActorStateRepository repository, ForgeMutationReuse reuse,
            ForgeMutationQueue submission, ForgeSpawnMutationRules spawnRules,
            NpcSpawnPositionValidator positionValidator, DiagnosticSink diagnostics, Clock clock) {
        this.ready = java.util.Objects.requireNonNull(ready, "ready");
        this.catalogState = java.util.Objects.requireNonNull(catalogState, "catalogState");
        this.runtime = java.util.Objects.requireNonNull(runtime, "runtime");
        this.materializer = java.util.Objects.requireNonNull(materializer, "materializer");
        this.repository = java.util.Objects.requireNonNull(repository, "repository");
        this.reuse = java.util.Objects.requireNonNull(reuse, "reuse");
        this.submission = java.util.Objects.requireNonNull(submission, "submission");
        this.spawnRules = java.util.Objects.requireNonNull(spawnRules, "spawnRules");
        this.positionValidator = java.util.Objects.requireNonNull(positionValidator, "positionValidator");
        this.diagnostics = java.util.Objects.requireNonNull(diagnostics, "diagnostics");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    public NpcResults.Spawn spawn(SpawnNpcRequest request) {
        if (!ready.getAsBoolean()) {
            return new NpcResults.Spawn(NpcResults.SpawnStatus.SERVICE_STOPPED, Optional.empty(),
                    "Сервис еще не активен.");
        }
        Optional<NpcResults.Spawn> duplicate = request.mutationId().flatMap(id -> reuse.spawn(id,
                request.actorId()));
        if (duplicate.isPresent()) {
            return duplicate.orElseThrow();
        }
        Optional<NpcResults.Spawn> existing = spawnRules.existing(request);
        if (existing.isPresent()) {
            return existing.orElseThrow();
        }
        Optional<NpcDefinition> definition = catalogState.current().find(request.definitionId());
        return definition.map(value -> queueSpawn(request, value))
                .orElseGet(() -> new NpcResults.Spawn(NpcResults.SpawnStatus.DEFINITION_NOT_FOUND,
                        Optional.empty(), "NpcDefinitionId не найден."));
    }

    public NpcResults.Teleport teleport(TeleportNpcRequest request) {
        if (!ready.getAsBoolean()) {
            return new NpcResults.Teleport(NpcResults.TeleportStatus.SERVICE_STOPPED, Optional.empty(),
                    "Сервис еще не активен.");
        }
        Optional<NpcResults.Teleport> duplicate = request.mutationId().flatMap(id -> reuse.teleport(id,
                request.actorId()));
        if (duplicate.isPresent()) {
            return duplicate.orElseThrow();
        }
        return runtime.actor(request.actorId()).map(actor -> queueTeleport(request, actor))
                .orElseGet(() -> new NpcResults.Teleport(NpcResults.TeleportStatus.NOT_FOUND,
                        Optional.empty(), "Актер не найден."));
    }

    public NpcResults.Despawn despawn(ActorId actorId, Optional<ActorMutationId> mutationId) {
        if (!ready.getAsBoolean()) {
            return new NpcResults.Despawn(NpcResults.DespawnStatus.SERVICE_STOPPED, "Сервис еще не активен.");
        }
        Optional<NpcResults.Despawn> duplicate = mutationId.flatMap(id -> reuse.despawn(id, actorId));
        if (duplicate.isPresent()) {
            return duplicate.orElseThrow();
        }
        Optional<ActiveActor> actor = runtime.actor(actorId);
        return actor.map(value -> queueDespawn(value, mutationId)).orElseGet(() -> new NpcResults.Despawn(
                NpcResults.DespawnStatus.ALREADY_ABSENT, "Актер уже отсутствует."));
    }

    private NpcResults.Spawn queueSpawn(SpawnNpcRequest request, NpcDefinition definition) {
        if (!materializer.worldExists(request.worldId())) {
            return new NpcResults.Spawn(NpcResults.SpawnStatus.WORLD_NOT_FOUND, Optional.empty(), "Мир не найден.");
        }
        var validation = positionValidator.validate(request.worldId(), request.position());
        if (!validation.accepted()) {
            diagnostics.accept(NpcDiagnostic.error(validation.diagnosticCode(), validation.message(),
                    request.actorId().value()));
            return new NpcResults.Spawn(NpcResults.SpawnStatus.INVALID_REQUEST, Optional.empty(),
                    validation.diagnosticCode());
        }
        Optional<NpcFormDefinition> initialForm = defaultForm(definition);
        if (definition.defaultFormId().isPresent() && initialForm.isEmpty()) {
            return new NpcResults.Spawn(NpcResults.SpawnStatus.DEFINITION_NOT_FOUND, Optional.empty(),
                    "NPC_FORM_NOT_FOUND");
        }
        ActorDesiredState base = spawnRules.desired(request, definition);
        ActorDesiredState desired = initialForm.map(base::withInitialForm).orElse(base);
        var mutation = new ForgePendingMutation(NpcResults.OperationKind.SPAWN, desired,
                Optional.of(definition), Optional.empty());
        String id = submission.submit(mutation, request.mutationId(), () -> repository.create(desired));
        return submission.queuedSpawn(request.actorId(), id);
    }

    private NpcResults.Teleport queueTeleport(TeleportNpcRequest request, ActiveActor actor) {
        if (actor.desired().restAttachment().isPresent()) {
            diagnostics.accept(NpcDiagnostic.warning("NPC_MOVE_WHILE_SLEEPING",
                    "Sleeping NPC movement rejected.", request.actorId().value()));
            return new NpcResults.Teleport(NpcResults.TeleportStatus.REJECTED,
                    Optional.of(actor.view()), "NPC_MOVE_WHILE_SLEEPING");
        }
        if (!materializer.worldExists(request.worldId())) {
            return new NpcResults.Teleport(NpcResults.TeleportStatus.WORLD_NOT_FOUND,
                    Optional.of(actor.view()), "Мир не найден.");
        }
        if (actor.definition().isEmpty()) {
            return new NpcResults.Teleport(NpcResults.TeleportStatus.REJECTED,
                    Optional.of(actor.view()), "Definition заблокирована.");
        }
        ActorDesiredState desired = actor.desired().withTarget(request.worldId(), request.position(),
                request.rotation(), clock.millis());
        var mutation = new ForgePendingMutation(NpcResults.OperationKind.TELEPORT, desired,
                actor.definition(), actor.handle());
        String id = submission.submit(mutation, request.mutationId(),
                () -> repository.update(desired, actor.desired().repositoryRevision()));
        return new NpcResults.Teleport(NpcResults.TeleportStatus.QUEUED, Optional.of(actor.view()),
                Optional.of(id), "Teleport сохранится перед перемещением.");
    }

    private NpcResults.Despawn queueDespawn(ActiveActor active, Optional<ActorMutationId> mutationId) {
        ActorDesiredState absent = active.desired().absent(clock.millis());
        var mutation = new ForgePendingMutation(NpcResults.OperationKind.DESPAWN, absent,
                active.definition(), active.handle(), active.desired().restAttachment());
        String id = submission.submit(mutation, mutationId,
                () -> repository.tombstone(absent, active.desired().repositoryRevision()));
        return new NpcResults.Despawn(NpcResults.DespawnStatus.QUEUED, Optional.of(id),
                "Despawn сохранит tombstone перед удалением entity.");
    }

    private Optional<NpcFormDefinition> defaultForm(NpcDefinition definition) {
        return definition.defaultFormId().flatMap(id -> catalogState.current().findForm(id));
    }
}
