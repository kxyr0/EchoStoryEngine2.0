package io.github.kxyr0.echonpc.forge.operation;

import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormDefinition;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.actor.ActorRuntime;
import io.github.kxyr0.echonpc.core.actor.ActiveActor;
import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import io.github.kxyr0.echonpc.core.definition.NpcCatalogState;
import io.github.kxyr0.echonpc.core.diagnostic.DiagnosticSink;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.core.persistence.ActorRepositoryResult;
import io.github.kxyr0.echonpc.core.platform.PlatformActorHandle;
import io.github.kxyr0.echonpc.forge.host.ForgeActorMaterializer;
import io.github.kxyr0.echonpc.forge.host.ForgeNpcOperationBook;
import io.github.kxyr0.echonpc.forge.rest.ForgeRestRuntime;
import java.util.Optional;

/**
 * Applies completed mutation I/O results on the server thread.
 */
public final class ForgeNpcMutationFlow {
    private final ActorRuntime runtime;
    private final NpcCatalogState catalogState;
    private final ForgeActorMaterializer materializer;
    private final ForgeRestRuntime restRuntime;
    private final ForgeNpcOperationBook operations;
    private final DiagnosticSink diagnostics;

    public ForgeNpcMutationFlow(ActorRuntime runtime, NpcCatalogState catalogState, ForgeActorMaterializer materializer,
            ForgeRestRuntime restRuntime, ForgeNpcOperationBook operations, DiagnosticSink diagnostics) {
        this.runtime = java.util.Objects.requireNonNull(runtime, "runtime");
        this.catalogState = java.util.Objects.requireNonNull(catalogState, "catalogState");
        this.materializer = java.util.Objects.requireNonNull(materializer, "materializer");
        this.restRuntime = java.util.Objects.requireNonNull(restRuntime, "restRuntime");
        this.operations = java.util.Objects.requireNonNull(operations, "operations");
        this.diagnostics = java.util.Objects.requireNonNull(diagnostics, "diagnostics");
    }

    public boolean apply(String operationId, ForgePendingMutation mutation, Object result) {
        if (result instanceof ActorRepositoryResult.Save save) {
            return applySave(operationId, mutation, save);
        }
        if (result instanceof ActorRepositoryResult.Removal removal) {
            return applyRemoval(operationId, mutation, removal);
        }
        operations.failed(operationId, Optional.empty(), "Неожиданный result persistence worker.");
        return true;
    }

    public void materializeSaved(ActorDesiredState desired, NpcDefinition definition,
            Optional<PlatformActorHandle> previous) {
        Optional<NpcFormDefinition> form = activeForm(desired, definition);
        ForgeActorMaterializer.Result result = materializer.materialize(desired, definition,
                form, previous);
        Optional<String> restFailure = restFailure(desired, result);
        if (restFailure.isPresent()) {
            runtime.updateDesired(desired, Optional.of(definition), NpcActorView.Status.PLATFORM_FAILURE,
                    restFailure);
            diagnostics.accept(NpcDiagnostic.error(restFailure.orElseThrow(),
                    "Rest attachment could not be applied.", desired.actorId().value()));
            return;
        }
        runtime.updateDesired(desired, Optional.of(definition), result.status(), reason(result));
        result.handle().ifPresent(handle -> runtime.markMaterialized(desired.actorId(), handle));
        diagnostics.accept(NpcDiagnostic.info("ENPC-ACTOR-MATERIALIZED", "Actor materialization reconciled.",
                desired.actorId().value() + " status=" + result.status()));
        form.ifPresent(value -> diagnostics.accept(NpcDiagnostic.info("ENPC-FORM-MATERIALIZED",
                "NPC form materialized.",
                "actor=" + desired.actorId().value()
                        + " form=" + value.id().value()
                        + " scale=" + value.bodyGeometry().globalScale()
                        + " dimensions=" + value.dimensions()
                        + " status=" + result.status()
                        + " handle=" + result.handle().map(PlatformActorHandle::value).orElse("<none>"))));
    }

    public void reconcileDormantActors(int limit) {
        runtime.listActors(limit).actors().stream()
                .filter(actor -> actor.status() == NpcActorView.Status.DORMANT_CHUNK_UNLOADED)
                .forEach(actor -> runtime.actor(actor.actorId()).ifPresent(this::tryDormantActor));
    }

    private boolean applySave(String operationId, ForgePendingMutation mutation, ActorRepositoryResult.Save save) {
        if (save.status() == ActorRepositoryResult.SaveStatus.REVISION_CONFLICT
                || save.status() == ActorRepositoryResult.SaveStatus.FAILED || save.actor().isEmpty()) {
            operations.failed(operationId, Optional.empty(), save.message());
            return true;
        }
        ActorDesiredState saved = save.actor().orElseThrow();
        runtime.updateDesired(saved, mutation.definition(), pendingStatus(mutation.kind()), Optional.empty());
        mutation.releasedRest().ifPresent(rest -> restRuntime.release(rest, mutation.handle()));
        materializeSaved(saved, mutation.definition().orElseThrow(), mutation.handle());
        operations.succeeded(operationId, runtime.query(saved.actorId()).actor(), "Durable save применен.");
        return false;
    }

    private boolean applyRemoval(String operationId, ForgePendingMutation mutation,
            ActorRepositoryResult.Removal removal) {
        if (removal.status() != ActorRepositoryResult.RemovalStatus.TOMBSTONED) {
            operations.failed(operationId, runtime.query(mutation.desired().actorId()).actor(), removal.message());
            return true;
        }
        mutation.releasedRest().ifPresent(rest -> restRuntime.release(rest, mutation.handle()));
        materializer.removeActor(mutation.desired().actorId(), mutation.handle());
        runtime.remove(mutation.desired().actorId());
        operations.succeeded(operationId, Optional.empty(), "Tombstone сохранен, entity удалена.");
        return false;
    }

    private void tryDormantActor(ActiveActor actor) {
        if (!materializer.chunkLoaded(actor.desired().worldId(), actor.desired().position())) {
            return;
        }
        actor.definition().ifPresent(definition -> materializeSaved(actor.desired(), definition, actor.handle()));
    }

    private Optional<String> reason(ForgeActorMaterializer.Result result) {
        return result.status() == NpcActorView.Status.PLATFORM_FAILURE ? Optional.of(result.message())
                : Optional.empty();
    }

    private Optional<NpcFormDefinition> activeForm(ActorDesiredState desired, NpcDefinition definition) {
        Optional<NpcFormId> id = desired.activeFormId().or(definition::defaultFormId);
        Optional<NpcFormDefinition> form = id.flatMap(value -> catalogState.current().findForm(value));
        if (id.isPresent() && form.isEmpty()) {
            diagnostics.accept(NpcDiagnostic.warning("NPC_FORM_NOT_FOUND",
                    "Active/default form не найдена при materialization.", id.orElseThrow().value()));
        }
        return form;
    }

    private Optional<String> restFailure(ActorDesiredState desired, ForgeActorMaterializer.Result result) {
        if (result.status() != NpcActorView.Status.MATERIALIZED) {
            return Optional.empty();
        }
        return restRuntime.apply(desired, result.handle());
    }

    private NpcActorView.Status pendingStatus(NpcResults.OperationKind kind) {
        return switch (kind) {
            case SPAWN -> NpcActorView.Status.PENDING_SPAWN;
            case DESPAWN -> NpcActorView.Status.PENDING_REMOVAL;
            case TELEPORT, SLEEP, WAKE, PLACE -> NpcActorView.Status.PENDING_MOVE;
            case FORM_CHANGE, SKIN_CHANGE -> NpcActorView.Status.PENDING_MOVE;
            case RECOVERY_RETRY -> NpcActorView.Status.PENDING_SPAWN;
        };
    }
}
