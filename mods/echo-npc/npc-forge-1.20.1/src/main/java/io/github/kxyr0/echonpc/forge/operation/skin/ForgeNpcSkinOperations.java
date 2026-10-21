package io.github.kxyr0.echonpc.forge.operation.skin;

import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeRequest;
import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeResult;
import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeStatus;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.actor.ActorPresence;
import io.github.kxyr0.echonpc.core.actor.ActorRuntime;
import io.github.kxyr0.echonpc.core.actor.ActiveActor;
import io.github.kxyr0.echonpc.core.persistence.ActorStateRepository;
import io.github.kxyr0.echonpc.forge.host.ForgeActorMaterializer;
import io.github.kxyr0.echonpc.forge.host.ForgeNpcOperationBook;
import io.github.kxyr0.echonpc.forge.operation.ForgeMutationQueue;
import io.github.kxyr0.echonpc.forge.operation.ForgePendingMutation;
import java.time.Clock;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Durable server-side implementation of Npc.ChangeSkin.
 */
public final class ForgeNpcSkinOperations {
    private final BooleanSupplier ready;
    private final ActorRuntime runtime;
    private final ForgeActorMaterializer materializer;
    private final ActorStateRepository repository;
    private final ForgeNpcOperationBook operations;
    private final ForgeMutationQueue submission;
    private final Clock clock;

    public ForgeNpcSkinOperations(BooleanSupplier ready, ActorRuntime runtime, ForgeActorMaterializer materializer,
            ActorStateRepository repository, ForgeNpcOperationBook operations, ForgeMutationQueue submission,
            Clock clock) {
        this.ready = java.util.Objects.requireNonNull(ready, "ready");
        this.runtime = java.util.Objects.requireNonNull(runtime, "runtime");
        this.materializer = java.util.Objects.requireNonNull(materializer, "materializer");
        this.repository = java.util.Objects.requireNonNull(repository, "repository");
        this.operations = java.util.Objects.requireNonNull(operations, "operations");
        this.submission = java.util.Objects.requireNonNull(submission, "submission");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    public NpcSkinChangeResult changeSkin(NpcSkinChangeRequest request) {
        if (!ready.getAsBoolean()) {
            return result(NpcSkinChangeStatus.SERVICE_STOPPED, request, Optional.empty(), "Сервис еще не активен.");
        }
        Optional<NpcSkinChangeResult> reused = reused(request);
        if (reused.isPresent()) {
            return reused.orElseThrow();
        }
        Optional<ActiveActor> actor = runtime.actor(request.npcId());
        if (actor.isEmpty()) {
            return result(NpcSkinChangeStatus.NOT_FOUND, request, Optional.empty(), "Актер не найден.");
        }
        return changeExisting(request, actor.orElseThrow());
    }

    private NpcSkinChangeResult changeExisting(NpcSkinChangeRequest request, ActiveActor actor) {
        if (actor.definition().isEmpty() || actor.desired().presence() != ActorPresence.ACTIVE) {
            return result(NpcSkinChangeStatus.REJECTED, request, Optional.of(actor.view()),
                    "Актер недоступен для смены скина.");
        }
        if (actor.desired().activeSkinId().filter(request.skinId()::equals).isPresent()) {
            actor.handle().flatMap(materializer::findEntity)
                    .ifPresent(entity -> entity.applySkin(request.skinId()));
            return result(NpcSkinChangeStatus.ALREADY_ACTIVE, request, Optional.of(actor.view()),
                    "Скин уже активен; live entity синхронизирована.");
        }
        ActorDesiredState desired = actor.desired().withSkin(request.skinId(), clock.millis());
        var mutation = new ForgePendingMutation(NpcResults.OperationKind.SKIN_CHANGE, desired,
                actor.definition(), actor.handle());
        String operationId = request.mutationId().map(id -> submission.submit(id.value(), mutation,
                () -> repository.update(desired, actor.desired().repositoryRevision())))
                .orElseGet(() -> submission.submit(mutation, Optional.empty(),
                        () -> repository.update(desired, actor.desired().repositoryRevision())));
        return submission.queuedSkinChange(request, operationId);
    }

    private Optional<NpcSkinChangeResult> reused(NpcSkinChangeRequest request) {
        if (request.mutationId().isEmpty()) {
            return Optional.empty();
        }
        String id = request.mutationId().orElseThrow().value();
        Optional<NpcResults.Operation> known = operations.query(id).operation();
        if (known.isEmpty()) {
            return Optional.empty();
        }
        NpcResults.Operation operation = known.orElseThrow();
        if (operation.kind() != NpcResults.OperationKind.SKIN_CHANGE
                || !operation.actorId().equals(request.npcId())) {
            return Optional.of(new NpcSkinChangeResult(NpcSkinChangeStatus.REJECTED, request.skinId(),
                    operation.actor(), Optional.of(id), "ActorMutationId уже занят другой операцией."));
        }
        return Optional.of(reusedResult(request, operation, id));
    }

    private NpcSkinChangeResult reusedResult(NpcSkinChangeRequest request, NpcResults.Operation operation,
            String operationId) {
        return switch (operation.status()) {
            case QUEUED, RUNNING -> new NpcSkinChangeResult(NpcSkinChangeStatus.QUEUED, request.skinId(),
                    runtime.query(request.npcId()).actor(), Optional.of(operationId),
                    "Npc.ChangeSkin mutation уже выполняется.");
            case SUCCEEDED -> new NpcSkinChangeResult(NpcSkinChangeStatus.COMMITTED, request.skinId(),
                    operation.actor(), Optional.of(operationId), "Npc.ChangeSkin mutation уже завершена.");
            case REJECTED -> new NpcSkinChangeResult(NpcSkinChangeStatus.REJECTED, request.skinId(),
                    operation.actor(), Optional.of(operationId), operation.message());
            case FAILED -> new NpcSkinChangeResult(NpcSkinChangeStatus.FAILURE, request.skinId(),
                    operation.actor(), Optional.of(operationId), operation.message());
        };
    }

    private NpcSkinChangeResult result(NpcSkinChangeStatus status, NpcSkinChangeRequest request,
            Optional<io.github.kxyr0.echonpc.api.service.NpcActorView> actor, String message) {
        return new NpcSkinChangeResult(status, request.skinId(), actor, message);
    }
}
