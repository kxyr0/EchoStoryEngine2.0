package io.github.kxyr0.echonpc.forge.recovery;

import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormDefinition;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.actor.ActorPresence;
import io.github.kxyr0.echonpc.core.actor.ActorRuntime;
import io.github.kxyr0.echonpc.core.actor.ActiveActor;
import io.github.kxyr0.echonpc.core.definition.NpcCatalogState;
import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import io.github.kxyr0.echonpc.core.diagnostic.DiagnosticSink;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.core.persistence.ActorRepositoryResult;
import io.github.kxyr0.echonpc.forge.host.ForgeActorMaterializer;
import io.github.kxyr0.echonpc.forge.operation.ForgeNpcMutationFlow;
import io.github.kxyr0.echonpc.forge.persistence.PersistenceWorkQueue;
import java.util.Optional;

/**
 * Applies startup actor recovery on the server thread.
 */
public final class ForgeNpcRecoveryFlow {
    private final NpcCatalogState catalogState;
    private final ActorRuntime runtime;
    private final ForgeActorMaterializer materializer;
    private final ForgeNpcMutationFlow mutations;
    private final DiagnosticSink diagnostics;

    public ForgeNpcRecoveryFlow(NpcCatalogState catalogState, ActorRuntime runtime,
            ForgeActorMaterializer materializer, ForgeNpcMutationFlow mutations, DiagnosticSink diagnostics) {
        this.catalogState = java.util.Objects.requireNonNull(catalogState, "catalogState");
        this.runtime = java.util.Objects.requireNonNull(runtime, "runtime");
        this.materializer = java.util.Objects.requireNonNull(materializer, "materializer");
        this.mutations = java.util.Objects.requireNonNull(mutations, "mutations");
        this.diagnostics = java.util.Objects.requireNonNull(diagnostics, "diagnostics");
    }

    public boolean applyStartup(PersistenceWorkQueue.Completed completed) {
        if (completed.failed() || !(completed.result() instanceof ActorRepositoryResult.LoadAll loaded)) {
            diagnostics.accept(NpcDiagnostic.error("ENPC-RECOVERY-FAILED", "Actor recovery failed.", "startup"));
            return true;
        }
        diagnostics.accept(NpcDiagnostic.info("ENPC-RECOVERY-STARTED", "Actor recovery started.",
                "records=" + loaded.records().size()));
        boolean degraded = false;
        for (ActorRepositoryResult.Load load : loaded.records()) {
            degraded |= recoverLoaded(load);
        }
        return degraded;
    }

    public Optional<NpcDefinition> compatibleDefinition(ActorDesiredState desired) {
        Optional<NpcDefinition> definition = catalogState.current().find(desired.definitionId());
        if (definition.isEmpty() || !definition.orElseThrow().fingerprint().equals(desired.definitionFingerprint())) {
            return Optional.empty();
        }
        return definition;
    }

    public Optional<NpcDefinition> recoverableDefinition(ActorDesiredState desired) {
        return catalogState.current().find(desired.definitionId());
    }

    public Optional<NpcFormDefinition> activeForm(ActorDesiredState desired, NpcDefinition definition) {
        Optional<NpcFormId> id = desired.activeFormId().or(definition::defaultFormId);
        return id.flatMap(value -> catalogState.current().findForm(value));
    }

    public NpcResults.Spawn retry(ActorId actorId) {
        Optional<ActiveActor> actor = runtime.actor(actorId);
        if (actor.isEmpty()) {
            return new NpcResults.Spawn(NpcResults.SpawnStatus.REJECTED, Optional.empty(),
                    "Актёр не найден.");
        }
        ActiveActor active = actor.orElseThrow();
        Optional<NpcDefinition> definition = recoverableDefinition(active.desired());
        if (definition.isEmpty()) {
            return new NpcResults.Spawn(NpcResults.SpawnStatus.REJECTED, Optional.of(active.view()),
                    "Definition по-прежнему отсутствует.");
        }
        mutations.materializeSaved(active.desired(), definition.orElseThrow(), active.handle());
        return new NpcResults.Spawn(NpcResults.SpawnStatus.CREATED, runtime.query(actorId).actor(),
                "Retry выполнен.");
    }

    private boolean recoverLoaded(ActorRepositoryResult.Load load) {
        if (load.status() != ActorRepositoryResult.LoadStatus.FOUND || load.actor().isEmpty()) {
            diagnostics.accept(NpcDiagnostic.warning("ENPC-ACTOR-RECORD-BLOCKED",
                    "Actor record не восстановлен.", load.message()));
            return true;
        }
        ActorDesiredState desired = load.actor().orElseThrow();
        if (desired.presence() == ActorPresence.ABSENT) {
            return false;
        }
        Optional<NpcDefinition> definition = recoverableDefinition(desired);
        if (definition.isEmpty()) {
            installBlocked(desired);
            return true;
        }
        if (!materializer.worldExists(desired.worldId())) {
            runtime.install(desired, definition, NpcActorView.Status.BLOCKED_WORLD_MISSING,
                    Optional.of("Мир отсутствует."));
            return true;
        }
        runtime.install(desired, definition, NpcActorView.Status.PENDING_SPAWN, Optional.empty());
        mutations.materializeSaved(desired, definition.orElseThrow(), Optional.empty());
        return false;
    }

    private void installBlocked(ActorDesiredState desired) {
        Optional<NpcDefinition> definition = catalogState.current().find(desired.definitionId());
        NpcActorView.Status status = definition.isEmpty() ? NpcActorView.Status.BLOCKED_DEFINITION_MISSING
                : NpcActorView.Status.BLOCKED_DEFINITION_INCOMPATIBLE;
        runtime.install(desired, Optional.empty(), status, Optional.of("Definition отсутствует или несовместима."));
    }
}
