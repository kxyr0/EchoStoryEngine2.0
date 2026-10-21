package io.github.kxyr0.echonpc.forge.host;

import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeRequest;
import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeResult;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.form.NpcFormCatalogView;
import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormChangeRequest;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormChangeResult;
import io.github.kxyr0.echonpc.api.rest.NpcRestProviderRegistry;
import io.github.kxyr0.echonpc.api.rest.command.NpcSleepRequest;
import io.github.kxyr0.echonpc.api.rest.command.NpcWakeRequest;
import io.github.kxyr0.echonpc.api.scene.NpcPlaceRequest;
import io.github.kxyr0.echonpc.api.scene.NpcPlaceResult;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.api.service.NpcServiceHealth;
import io.github.kxyr0.echonpc.api.service.SpawnNpcRequest;
import io.github.kxyr0.echonpc.api.service.TeleportNpcRequest;
import io.github.kxyr0.echonpc.core.actor.ActorRuntime;
import io.github.kxyr0.echonpc.core.actor.ActiveActor;
import io.github.kxyr0.echonpc.core.definition.NpcCatalogState;
import io.github.kxyr0.echonpc.core.diagnostic.DiagnosticSink;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.core.persistence.ActorStateRepository;
import io.github.kxyr0.echonpc.forge.entity.EchoNpcEntity;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBody;
import io.github.kxyr0.echonpc.forge.operation.ForgeNpcMutationFlow;
import io.github.kxyr0.echonpc.forge.operation.ForgeNpcMutationService;
import io.github.kxyr0.echonpc.forge.persistence.ForgePersistencePaths;
import io.github.kxyr0.echonpc.forge.persistence.PersistenceWorkQueue;
import io.github.kxyr0.echonpc.forge.recovery.ForgeNpcRecoveryFlow;
import io.github.kxyr0.echonpc.forge.rest.ForgeRestRuntime;
import io.github.kxyr0.echonpc.forge.spawn.NpcSpawnPositionValidator;
import io.github.kxyr0.echonpc.persistence.file.ActorFileRepository;
import java.time.Clock;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

public final class ForgeNpcHost implements NpcService {
    private static final int LOAD_LIMIT = 2048;
    private final NpcCatalogState catalogState;
    private final DiagnosticSink diagnostics;
    private final ActorRuntime runtime;
    private final ForgeActorMaterializer materializer;
    private final ForgeRestRuntime restRuntime;
    private final ForgeNpcOperationBook operations = new ForgeNpcOperationBook();
    private final PersistenceWorkQueue queue;
    private final ActorStateRepository repository;
    private final ForgeNpcMutationFlow mutations;
    private final ForgeNpcRecoveryFlow recovery;
    private final ForgeNpcMutationService mutationService;
    private final ForgeNpcEntityLinker entityLinker;
    private boolean active, loading, degraded;

    public ForgeNpcHost(MinecraftServer server, NpcCatalogState catalogState, DiagnosticSink diagnostics) {
        this(server, catalogState, diagnostics, Clock.systemUTC());
    }

    public ForgeNpcHost(MinecraftServer server, NpcCatalogState catalogState, DiagnosticSink diagnostics, Clock clock) {
        MinecraftServer checkedServer = java.util.Objects.requireNonNull(server, "server");
        this.catalogState = java.util.Objects.requireNonNull(catalogState, "catalogState");
        this.diagnostics = java.util.Objects.requireNonNull(diagnostics, "diagnostics");
        Clock checkedClock = java.util.Objects.requireNonNull(clock, "clock");
        ForgeNpcEntityIndex index = new ForgeNpcEntityIndex();
        this.runtime = new ActorRuntime(diagnostics);
        this.materializer = new ForgeActorMaterializer(checkedServer, index);
        this.restRuntime = new ForgeRestRuntime(checkedServer, materializer);
        this.repository = new ActorFileRepository(ForgePersistencePaths.actorsRoot(checkedServer));
        this.mutations = new ForgeNpcMutationFlow(runtime, catalogState, materializer, restRuntime, operations,
                diagnostics);
        this.recovery = new ForgeNpcRecoveryFlow(catalogState, runtime, materializer, mutations, diagnostics);
        String threadName = "EchoNpcPersistence-" + Integer.toHexString(System.identityHashCode(checkedServer));
        this.queue = new PersistenceWorkQueue(threadName);
        this.mutationService = new ForgeNpcMutationService(() -> active && !loading, catalogState, runtime,
                materializer, repository, queue, operations, recovery, mutations, restRuntime,
                new NpcSpawnPositionValidator(checkedServer), diagnostics, checkedClock);
        this.entityLinker = new ForgeNpcEntityLinker(runtime, materializer, recovery, diagnostics);
    }

    public void start() {
        if (active) {
            return;
        }
        runtime.start();
        active = true;
        loading = true;
        diagnostics.accept(NpcDiagnostic.info("ENPC-PERSISTENCE-ROOT", "Persistence root initialized.",
                "actors"));
        if (!queue.submit("__startup_load", () -> repository.loadAll(LOAD_LIMIT))) {
            degraded = true;
            loading = false;
            diagnostics.accept(NpcDiagnostic.error("ENPC-PERSISTENCE-QUEUE-FULL",
                    "Persistence queue saturated during startup.", "loadAll"));
        }
    }

    public void stop() {
        active = false;
        runtime.listActors(LOAD_LIMIT).actors().forEach(actor -> runtime.actor(actor.actorId())
                .flatMap(ActiveActor::handle)
                .ifPresent(handle -> materializer.removeActor(actor.actorId(), Optional.of(handle))));
        boolean clean = queue.shutdown(5_000L);
        if (!clean) {
            diagnostics.accept(NpcDiagnostic.warning("ENPC-PERSISTENCE-SHUTDOWN-TIMEOUT",
                    "Persistence worker shutdown timeout.", "timeoutMillis=5000"));
        }
        runtime.stop();
        mutationService.clear();
    }

    public NpcService service() { return this; }

    public void tick() {
        for (PersistenceWorkQueue.Completed completed : queue.drain(PersistenceWorkQueue.CAPACITY)) {
            applyCompleted(completed);
        }
        if (!loading && active) {
            mutations.reconcileDormantActors(LOAD_LIMIT);
        }
    }

    @Override
    public NpcResults.Spawn spawn(SpawnNpcRequest request) { return mutationService.spawn(request); }
    @Override
    public NpcResults.Query query(ActorId actorId) { return runtime.query(actorId); }
    @Override
    public NpcResults.ListActors listActors(int limit) { return runtime.listActors(limit); }
    @Override
    public NpcResults.Teleport teleport(TeleportNpcRequest request) { return mutationService.teleport(request); }
    @Override
    public NpcResults.Despawn despawn(ActorId actorId) { return mutationService.despawn(actorId); }
    @Override
    public NpcResults.Despawn despawn(ActorId actorId, ActorMutationId mutationId) {
        return mutationService.despawn(actorId, mutationId);
    }
    @Override
    public NpcResults.Sleep sleepAt(NpcSleepRequest request) { return mutationService.sleepAt(request); }
    @Override
    public NpcResults.Wake wakeUp(NpcWakeRequest request) { return mutationService.wakeUp(request); }
    @Override
    public NpcFormChangeResult setForm(NpcFormChangeRequest request) { return mutationService.setForm(request); }
    @Override
    public NpcSkinChangeResult changeSkin(NpcSkinChangeRequest request) { return mutationService.changeSkin(request); }
    @Override
    public NpcPlaceResult placeAt(NpcPlaceRequest request) { return mutationService.placeAt(request); }

    @Override
    public NpcFormCatalogView formCatalog(int limit) {
        var catalog = catalogState.current();
        int bounded = Math.max(0, Math.min(limit, 200));
        return new NpcFormCatalogView(catalog.revision(), catalog.formDefinitions(bounded),
                catalog.forms().size() > bounded);
    }

    public String validateFormSpace(ActorId actorId, NpcFormId formId) {
        Optional<ActiveActor> actor = runtime.actor(actorId);
        var form = catalogState.current().findForm(formId);
        if (actor.isEmpty()) {
            return "NPC_FORM_SPACE actor not found.";
        }
        if (form.isEmpty()) {
            return "NPC_FORM_NOT_FOUND";
        }
        return materializer.formFits(actor.orElseThrow(), form.orElseThrow())
                ? "NPC_FORM_SPACE_OK"
                : "NPC_FORM_SPACE_BLOCKED";
    }

    public NpcRestProviderRegistry restProviders() { return restRuntime.providers(); }

    @Override
    public NpcServiceHealth health() {
        var catalog = catalogState.current();
        return new NpcServiceHealth(active && !loading, catalog.revision(), catalog.definitions().size(),
                runtime.activeActorCount(), runtime.durableActorCount(), runtime.blockedCount(),
                queue.queuedCount() + operations.queuedCount(), degraded, false, false);
    }

    @Override
    public NpcResults.QueryOperation operation(String operationId) { return operations.query(operationId); }

    @Override
    public NpcResults.ListOperations listOperations(int limit) { return operations.list(limit); }

    @Override
    public NpcResults.ListActors blockedActors(int limit) { return runtime.blockedActors(limit); }

    @Override
    public NpcResults.Spawn retryRecovery(ActorId actorId) { return mutationService.retryRecovery(actorId); }

    public void reconcileLoadedEntity(EchoNpcEntity entity) {
        if (!active || loading || entity.level().isClientSide()) {
            return;
        }
        entityLinker.reconcile(entity);
    }

    public Optional<NpcNavigationBody> navigationBody(ActorId actorId) {
        if (!active || loading) {
            return Optional.empty();
        }
        return runtime.actor(actorId)
                .flatMap(ActiveActor::handle)
                .flatMap(handle -> materializer.findEntity(handle))
                .map(entity -> new NpcNavigationBody(actorId, (net.minecraft.server.level.ServerLevel) entity.level(),
                        entity));
    }

    private void applyCompleted(PersistenceWorkQueue.Completed completed) {
        if ("__startup_load".equals(completed.operationId())) {
            loading = false;
            degraded |= recovery.applyStartup(completed);
            return;
        }
        degraded |= mutationService.applyCompleted(completed);
    }

}
