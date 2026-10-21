package io.github.kxyr0.echonpc.forge.operation.form;

import io.github.kxyr0.echonpc.api.form.NpcFormOutcome;
import io.github.kxyr0.echonpc.api.form.NpcFormStatus;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormChangeRequest;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormChangeResult;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormDefinition;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.actor.ActorRuntime;
import io.github.kxyr0.echonpc.core.actor.ActiveActor;
import io.github.kxyr0.echonpc.core.definition.NpcCatalogState;
import io.github.kxyr0.echonpc.core.diagnostic.DiagnosticSink;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.core.persistence.ActorStateRepository;
import io.github.kxyr0.echonpc.forge.host.ForgeActorMaterializer;
import io.github.kxyr0.echonpc.forge.operation.ForgeMutationQueue;
import io.github.kxyr0.echonpc.forge.operation.ForgeMutationReuse;
import io.github.kxyr0.echonpc.forge.operation.ForgePendingMutation;
import java.time.Clock;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Durable server-side implementation of Npc.SetForm.
 */
public final class ForgeNpcFormOperations {
    private final BooleanSupplier ready;
    private final NpcCatalogState catalogState;
    private final ActorRuntime runtime;
    private final ForgeActorMaterializer materializer;
    private final ForgeNpcFormEntitySynchronizer formSync;
    private final ActorStateRepository repository;
    private final ForgeMutationReuse reuse;
    private final ForgeMutationQueue submission;
    private final DiagnosticSink diagnostics;
    private final Clock clock;

    public ForgeNpcFormOperations(BooleanSupplier ready, NpcCatalogState catalogState, ActorRuntime runtime,
            ForgeActorMaterializer materializer, ActorStateRepository repository, ForgeMutationReuse reuse,
            ForgeMutationQueue submission, DiagnosticSink diagnostics, Clock clock) {
        this.ready = java.util.Objects.requireNonNull(ready, "ready");
        this.catalogState = java.util.Objects.requireNonNull(catalogState, "catalogState");
        this.runtime = java.util.Objects.requireNonNull(runtime, "runtime");
        this.materializer = java.util.Objects.requireNonNull(materializer, "materializer");
        this.formSync = new ForgeNpcFormEntitySynchronizer(this.materializer);
        this.repository = java.util.Objects.requireNonNull(repository, "repository");
        this.reuse = java.util.Objects.requireNonNull(reuse, "reuse");
        this.submission = java.util.Objects.requireNonNull(submission, "submission");
        this.diagnostics = java.util.Objects.requireNonNull(diagnostics, "diagnostics");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    public NpcFormChangeResult setForm(NpcFormChangeRequest request) {
        if (!ready.getAsBoolean()) {
            return rejected(NpcFormStatus.SERVICE_STOPPED, Optional.empty(), "Сервис еще не активен.");
        }
        Optional<NpcFormChangeResult> duplicate = request.commandId()
                .flatMap(id -> reuse.formChange(id, request.npcId()));
        if (duplicate.isPresent()) {
            return duplicate.orElseThrow();
        }
        Optional<ActiveActor> actor = runtime.actor(request.npcId());
        if (actor.isEmpty()) {
            return rejected(NpcFormStatus.NOT_FOUND, Optional.empty(), "Актер не найден.");
        }
        Optional<NpcFormDefinition> form = catalogState.current().findForm(request.formId());
        if (form.isEmpty()) {
            diagnostics.accept(NpcDiagnostic.warning("NPC_FORM_NOT_FOUND",
                    "Npc.SetForm target form не найдена.", request.formId().value()));
            return rejected(NpcFormStatus.FORM_NOT_FOUND, Optional.of(actor.orElseThrow().view()),
                    "NPC_FORM_NOT_FOUND");
        }
        return queueOrReject(request, actor.orElseThrow(), form.orElseThrow());
    }

    public Optional<NpcFormDefinition> activeForm(ActiveActor actor) {
        return actor.desired().activeFormId()
                .or(() -> actor.definition().flatMap(definition -> definition.defaultFormId()))
                .flatMap(id -> catalogState.current().findForm(id));
    }

    private NpcFormChangeResult queueOrReject(
            NpcFormChangeRequest request,
            ActiveActor actor,
            NpcFormDefinition form
    ) {
        if (actor.desired().activeFormId().filter(request.formId()::equals).isPresent()) {
            ForgeNpcFormEntitySynchronizer.Result sync = formSync.sync(actor, form);
            diagnostics.accept(NpcDiagnostic.info("ENPC-FORM-ALREADY-ACTIVE-SYNC",
                    "Npc.SetForm target already active; live entity sync attempted.",
                    "actor=" + actor.desired().actorId().value()
                            + " form=" + form.id().value()
                            + " scale=" + form.bodyGeometry().globalScale()
                            + " dimensions=" + form.dimensions()
                            + " syncApplied=" + sync.applied()
                            + " " + sync.message()));
            return new NpcFormChangeResult(NpcFormStatus.ALREADY_ACTIVE, NpcFormOutcome.UNCHANGED,
                    Optional.of(actor.view()), Optional.of(form.snapshot()), Optional.empty(),
                    "Форма уже активна; entity sync=" + sync.applied() + ".");
        }
        Optional<NpcFormChangeResult> blocked = blocked(actor, form);
        if (blocked.isPresent()) {
            return blocked.orElseThrow();
        }
        actor.desired().restAttachment().ifPresent(rest -> diagnostics.accept(NpcDiagnostic.info(
                "ENPC-FORM-SLEEPING-QUEUED",
                "Npc.SetForm queued while durable rest attachment is retained.",
                "actor=" + actor.desired().actorId().value()
                        + " form=" + form.id().value()
                        + " restProvider=" + rest.providerId()
                        + " bed=" + rest.canonicalBedPosition())));
        ActorDesiredState desired = actor.desired().withForm(form, clock.millis());
        var mutation = new ForgePendingMutation(NpcResults.OperationKind.FORM_CHANGE, desired,
                actor.definition(), actor.handle());
        String id = request.commandId().map(value -> submission.submit(value.value(), mutation,
                () -> repository.update(desired, actor.desired().repositoryRevision())))
                .orElseGet(() -> submission.submit(mutation, Optional.empty(),
                        () -> repository.update(desired, actor.desired().repositoryRevision())));
        return submission.queuedFormChange(request.npcId(), id);
    }

    private Optional<NpcFormChangeResult> blocked(ActiveActor actor, NpcFormDefinition form) {
        if (actor.definition().isEmpty()) {
            return Optional.of(rejected(NpcFormStatus.REJECTED, Optional.of(actor.view()),
                    "Definition заблокирована."));
        }
        boolean sleeping = actor.desired().restAttachment().isPresent();
        if (!sleeping && (actor.status() == NpcActorView.Status.PENDING_MOVE
                || materializer.navigationInProgress(actor.handle()))) {
            return Optional.of(rejected(NpcFormStatus.WHILE_MOVING, Optional.of(actor.view()),
                    "NPC_FORM_CHANGE_WHILE_MOVING"));
        }
        if (!sleeping && !materializer.formFits(actor, form)) {
            return Optional.of(rejected(NpcFormStatus.SPACE_BLOCKED, Optional.of(actor.view()),
                    "NPC_FORM_SPACE_BLOCKED"));
        }
        return Optional.empty();
    }

    private NpcFormChangeResult rejected(NpcFormStatus status, Optional<NpcActorView> actor, String message) {
        return new NpcFormChangeResult(status, NpcFormOutcome.OLD_FORM_RETAINED, actor, message);
    }
}
