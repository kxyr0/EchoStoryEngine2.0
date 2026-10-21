package io.github.kxyr0.echonpc.forge.operation.place;

import io.github.kxyr0.echonpc.api.scene.NpcPlaceRequest;
import io.github.kxyr0.echonpc.api.scene.NpcPlaceResult;
import io.github.kxyr0.echonpc.api.scene.NpcPlaceStatus;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.actor.ActorRuntime;
import io.github.kxyr0.echonpc.core.actor.ActiveActor;
import io.github.kxyr0.echonpc.core.diagnostic.DiagnosticSink;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.core.persistence.ActorStateRepository;
import io.github.kxyr0.echonpc.forge.host.ForgeActorMaterializer;
import io.github.kxyr0.echonpc.forge.operation.ForgeMutationQueue;
import io.github.kxyr0.echonpc.forge.operation.ForgeMutationReuse;
import io.github.kxyr0.echonpc.forge.operation.ForgePendingMutation;
import io.github.kxyr0.echonpc.forge.operation.form.ForgeNpcFormOperations;
import io.github.kxyr0.echonpc.forge.spawn.NpcSpawnPositionValidator;
import java.time.Clock;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Durable server-side implementation of cinematic Npc.PlaceAt.
 */
public final class ForgeNpcPlaceOperations {
    private final BooleanSupplier ready;
    private final ActorRuntime runtime;
    private final ForgeActorMaterializer materializer;
    private final ActorStateRepository repository;
    private final ForgeMutationReuse reuse;
    private final ForgeMutationQueue submission;
    private final ForgeNpcFormOperations forms;
    private final NpcSpawnPositionValidator positionValidator;
    private final DiagnosticSink diagnostics;
    private final Clock clock;

    public ForgeNpcPlaceOperations(BooleanSupplier ready, ActorRuntime runtime, ForgeActorMaterializer materializer,
            ActorStateRepository repository, ForgeMutationReuse reuse, ForgeMutationQueue submission,
            ForgeNpcFormOperations forms, NpcSpawnPositionValidator positionValidator,
            DiagnosticSink diagnostics, Clock clock) {
        this.ready = java.util.Objects.requireNonNull(ready, "ready");
        this.runtime = java.util.Objects.requireNonNull(runtime, "runtime");
        this.materializer = java.util.Objects.requireNonNull(materializer, "materializer");
        this.repository = java.util.Objects.requireNonNull(repository, "repository");
        this.reuse = java.util.Objects.requireNonNull(reuse, "reuse");
        this.submission = java.util.Objects.requireNonNull(submission, "submission");
        this.forms = java.util.Objects.requireNonNull(forms, "forms");
        this.positionValidator = java.util.Objects.requireNonNull(positionValidator, "positionValidator");
        this.diagnostics = java.util.Objects.requireNonNull(diagnostics, "diagnostics");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    public NpcPlaceResult placeAt(NpcPlaceRequest request) {
        if (!ready.getAsBoolean()) {
            return new NpcPlaceResult(NpcPlaceStatus.SERVICE_STOPPED, Optional.empty(), "Сервис еще не активен.");
        }
        Optional<NpcPlaceResult> duplicate = request.commandId().flatMap(id -> reuse.place(id, request.npcId()));
        if (duplicate.isPresent()) {
            return duplicate.orElseThrow();
        }
        Optional<ActiveActor> actor = runtime.actor(request.npcId());
        if (actor.isEmpty()) {
            return new NpcPlaceResult(NpcPlaceStatus.NOT_FOUND, Optional.empty(), "Актер не найден.");
        }
        return queueOrReject(request, actor.orElseThrow());
    }

    private NpcPlaceResult queueOrReject(NpcPlaceRequest request, ActiveActor actor) {
        Optional<NpcPlaceResult> blocked = blocked(request, actor);
        if (blocked.isPresent()) {
            return blocked.orElseThrow();
        }
        ActorDesiredState desired = actor.desired().withTarget(request.worldId(), request.position(),
                request.rotation(), clock.millis());
        var mutation = new ForgePendingMutation(NpcResults.OperationKind.PLACE, desired,
                actor.definition(), actor.handle());
        String id = request.commandId().map(value -> submission.submit(value.value(), mutation,
                () -> repository.update(desired, actor.desired().repositoryRevision())))
                .orElseGet(() -> submission.submit(mutation, Optional.empty(),
                        () -> repository.update(desired, actor.desired().repositoryRevision())));
        return submission.queuedPlace(request.npcId(), id);
    }

    private Optional<NpcPlaceResult> blocked(NpcPlaceRequest request, ActiveActor actor) {
        if (actor.desired().restAttachment().isPresent()) {
            return Optional.of(result(NpcPlaceStatus.WHILE_SLEEPING, actor, "NPC_PLACE_WHILE_SLEEPING"));
        }
        if (actor.status() == NpcActorView.Status.PENDING_MOVE || actor.handle().isEmpty()) {
            return Optional.of(result(NpcPlaceStatus.NOT_MATERIALIZED, actor, "NPC_PLACE_NOT_MATERIALIZED"));
        }
        if (!materializer.worldExists(request.worldId())) {
            return Optional.of(result(NpcPlaceStatus.TARGET_INVALID, actor, "NPC_PLACE_TARGET_INVALID"));
        }
        var validation = positionValidator.validate(request.worldId(), request.position());
        if (!validation.accepted()) {
            diagnostics.accept(NpcDiagnostic.error(validation.diagnosticCode(), validation.message(),
                    request.npcId().value()));
            return Optional.of(result(NpcPlaceStatus.TARGET_INVALID, actor, validation.diagnosticCode()));
        }
        if (!materializer.targetFits(request.worldId(), request.position(), forms.activeForm(actor),
                actor.handle())) {
            return Optional.of(result(NpcPlaceStatus.COLLISION, actor, "NPC_PLACE_COLLISION"));
        }
        return Optional.empty();
    }

    private NpcPlaceResult result(NpcPlaceStatus status, ActiveActor actor, String message) {
        return new NpcPlaceResult(status, Optional.of(actor.view()), message);
    }
}
