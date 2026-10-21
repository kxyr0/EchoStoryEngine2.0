package io.github.kxyr0.echonpc.forge.operation;

import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.rest.command.NpcSleepRequest;
import io.github.kxyr0.echonpc.api.rest.command.NpcWakeRequest;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.actor.ActorRuntime;
import io.github.kxyr0.echonpc.core.actor.ActiveActor;
import io.github.kxyr0.echonpc.core.diagnostic.DiagnosticSink;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.core.persistence.ActorStateRepository;
import io.github.kxyr0.echonpc.core.rest.NpcRestAttachment;
import io.github.kxyr0.echonpc.forge.host.ForgeActorMaterializer;
import io.github.kxyr0.echonpc.forge.host.ForgeNpcOperationBook;
import io.github.kxyr0.echonpc.forge.rest.ForgeRestResolution;
import io.github.kxyr0.echonpc.forge.rest.ForgeRestRuntime;
import java.time.Clock;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * SleepAt and WakeUp command implementation backed by durable actor state.
 */
final class ForgeNpcRestOperations {
    private static final BlockId ANY_BED = new BlockId("echo_npc:any_bed");
    private final BooleanSupplier ready;
    private final ActorRuntime runtime;
    private final ForgeActorMaterializer materializer;
    private final ActorStateRepository repository;
    private final ForgeNpcOperationBook operations;
    private final ForgeMutationReuse reuse;
    private final ForgeMutationQueue submission;
    private final ForgeRestRuntime restRuntime;
    private final DiagnosticSink diagnostics;
    private final Clock clock;

    ForgeNpcRestOperations(BooleanSupplier ready, ActorRuntime runtime, ForgeActorMaterializer materializer,
            ActorStateRepository repository, ForgeNpcOperationBook operations, ForgeMutationReuse reuse,
            ForgeMutationQueue submission, ForgeRestRuntime restRuntime, DiagnosticSink diagnostics, Clock clock) {
        this.ready = java.util.Objects.requireNonNull(ready, "ready");
        this.runtime = java.util.Objects.requireNonNull(runtime, "runtime");
        this.materializer = java.util.Objects.requireNonNull(materializer, "materializer");
        this.repository = java.util.Objects.requireNonNull(repository, "repository");
        this.operations = java.util.Objects.requireNonNull(operations, "operations");
        this.reuse = java.util.Objects.requireNonNull(reuse, "reuse");
        this.submission = java.util.Objects.requireNonNull(submission, "submission");
        this.restRuntime = java.util.Objects.requireNonNull(restRuntime, "restRuntime");
        this.diagnostics = java.util.Objects.requireNonNull(diagnostics, "diagnostics");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    NpcResults.Sleep sleepAt(NpcSleepRequest request) {
        if (!ready.getAsBoolean()) {
            return new NpcResults.Sleep(NpcResults.SleepStatus.SERVICE_STOPPED, Optional.empty(),
                    "Сервис ещё не активен.");
        }
        Optional<NpcResults.Sleep> duplicate = request.mutationId()
                .flatMap(id -> reuse.sleep(id, request.npcId()));
        if (duplicate.isPresent()) {
            return duplicate.orElseThrow();
        }
        Optional<ActiveActor> actor = runtime.actor(request.npcId());
        if (actor.isEmpty()) {
            return rejected(request, NpcResults.SleepStatus.NOT_FOUND, "NPC_SLEEP_NPC_REMOVED");
        }
        return startSleep(request, actor.orElseThrow());
    }

    NpcResults.Wake wakeUp(NpcWakeRequest request) {
        if (!ready.getAsBoolean()) {
            return new NpcResults.Wake(NpcResults.WakeStatus.SERVICE_STOPPED, Optional.empty(),
                    "Сервис ещё не активен.");
        }
        Optional<NpcResults.Wake> duplicate = request.mutationId()
                .flatMap(id -> reuse.wake(id, request.npcId()));
        if (duplicate.isPresent()) {
            return duplicate.orElseThrow();
        }
        Optional<ActiveActor> actor = runtime.actor(request.npcId());
        return actor.map(value -> startWake(request, value)).orElseGet(() ->
                new NpcResults.Wake(NpcResults.WakeStatus.NOT_FOUND, Optional.empty(), "NPC not found."));
    }

    private NpcResults.Sleep startSleep(NpcSleepRequest request, ActiveActor actor) {
        if (actor.desired().restAttachment().isPresent()) {
            return existingSleep(request, actor, actor.desired().restAttachment().orElseThrow());
        }
        if (actor.definition().isEmpty() || actor.handle().isEmpty()) {
            return rejected(request, NpcResults.SleepStatus.NOT_MATERIALIZED, "NPC_SLEEP_NPC_NOT_MATERIALIZED");
        }
        if (actor.handle().filter(handle -> !materializer.exists(handle)).isPresent()) {
            return rejected(request, NpcResults.SleepStatus.NOT_MATERIALIZED, "NPC_SLEEP_NPC_NOT_MATERIALIZED");
        }
        String operationId = request.mutationId().map(ActorMutationId::value).orElseGet(operations::nextId);
        ForgeRestResolution resolved = restRuntime.resolve(request, operationId, actor.desired().actorRevision() + 1L);
        if (resolved.attachment().isEmpty()) {
            diagnostics.accept(NpcDiagnostic.error(resolved.diagnosticCode(), resolved.message(),
                    request.npcId().value()));
            return new NpcResults.Sleep(resolved.status(), Optional.of(actor.view()), resolved.message());
        }
        return submitSleep(actor, operationId, resolved.attachment().orElseThrow());
    }

    private NpcResults.Sleep submitSleep(ActiveActor actor, String operationId, NpcRestAttachment attachment) {
        if (runtime.restOccupied(attachment.canonicalBedPosition(), actor.desired().actorId())) {
            return new NpcResults.Sleep(NpcResults.SleepStatus.BED_OCCUPIED, Optional.of(actor.view()),
                    "NPC_SLEEP_BED_OCCUPIED");
        }
        ActorDesiredState desired = actor.desired().withRestAttachment(attachment, clock.millis());
        var mutation = new ForgePendingMutation(NpcResults.OperationKind.SLEEP, desired,
                actor.definition(), actor.handle());
        submission.submit(operationId, mutation, () -> repository.update(desired,
                actor.desired().repositoryRevision()));
        return submission.queuedSleep(actor.desired().actorId(), operationId);
    }

    private NpcResults.Wake startWake(NpcWakeRequest request, ActiveActor actor) {
        Optional<NpcRestAttachment> current = actor.desired().restAttachment();
        if (current.isEmpty()) {
            return new NpcResults.Wake(NpcResults.WakeStatus.ALREADY_AWAKE, Optional.of(actor.view()),
                    "NPC already awake.");
        }
        if (actor.definition().isEmpty()) {
            return new NpcResults.Wake(NpcResults.WakeStatus.REJECTED, Optional.of(actor.view()),
                    "Definition is unavailable.");
        }
        return submitWake(request, actor, current.orElseThrow());
    }

    private NpcResults.Wake submitWake(NpcWakeRequest request, ActiveActor actor, NpcRestAttachment rest) {
        try {
            NpcPosition wakePosition = restRuntime.wakePosition(rest);
            NpcRotation rotation = new NpcRotation(actor.desired().rotation().yaw(), 0.0F);
            ActorDesiredState desired = actor.desired().withoutRestAttachment(wakePosition, rotation, clock.millis());
            String operationId = request.mutationId().map(ActorMutationId::value).orElseGet(operations::nextId);
            var mutation = new ForgePendingMutation(NpcResults.OperationKind.WAKE, desired,
                    actor.definition(), actor.handle(), Optional.of(rest));
            submission.submit(operationId, mutation, () -> repository.update(desired,
                    actor.desired().repositoryRevision()));
            return submission.queuedWake(actor.desired().actorId(), operationId);
        } catch (IllegalArgumentException exception) {
            return new NpcResults.Wake(NpcResults.WakeStatus.POSITION_INVALID, Optional.of(actor.view()),
                    "NPC_WAKE_POSITION_INVALID");
        }
    }

    private NpcResults.Sleep existingSleep(NpcSleepRequest request, ActiveActor actor, NpcRestAttachment rest) {
        if (sameBed(request, rest)
                && (rest.expectedBlock().equals(request.expectedBlock()) || ANY_BED.equals(request.expectedBlock()))) {
            return new NpcResults.Sleep(NpcResults.SleepStatus.ALREADY_SLEEPING, Optional.of(actor.view()),
                    Optional.empty(), Optional.of(rest.view()), "NPC already sleeping at this bed.");
        }
        return new NpcResults.Sleep(NpcResults.SleepStatus.REJECTED, Optional.of(actor.view()),
                "NPC_SCENE_COMMAND_IDEMPOTENCY_CONFLICT");
    }

    private boolean sameBed(NpcSleepRequest request, NpcRestAttachment rest) {
        return rest.bedPosition().equals(request.bedPosition())
                || rest.canonicalBedPosition().equals(request.bedPosition());
    }

    private NpcResults.Sleep rejected(NpcSleepRequest request, NpcResults.SleepStatus status, String code) {
        diagnostics.accept(NpcDiagnostic.error(code, code, request.npcId().value()));
        return new NpcResults.Sleep(status, runtime.query(request.npcId()).actor(), code);
    }
}
