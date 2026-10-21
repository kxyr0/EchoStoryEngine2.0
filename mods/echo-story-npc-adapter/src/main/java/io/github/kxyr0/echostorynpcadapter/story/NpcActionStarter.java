package io.github.kxyr0.echostorynpcadapter.story;

import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.api.service.SpawnNpcRequest;
import io.github.kxyr0.echonpc.api.service.TeleportNpcRequest;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceLookupResult;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import io.github.kxyr0.echostorynpcadapter.action.ActionPayloadDecoder;
import io.github.kxyr0.echostorynpcadapter.action.DespawnPayload;
import io.github.kxyr0.echostorynpcadapter.action.NpcActionTypes;
import io.github.kxyr0.echostorynpcadapter.action.SpawnPayload;
import io.github.kxyr0.echostorynpcadapter.action.TeleportPayload;
import io.github.kxyr0.echostorynpcadapter.mutation.StoryMutationMapper;
import io.github.kxyr0.echostorynpcadapter.observe.ActionObservation;
import io.github.kxyr0.echostorynpcadapter.observe.ObservationBook;
import io.github.kxyr0.echostorynpcadapter.story.conflict.NpcStartConflicts;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

final class NpcActionStarter {
    private final MinecraftServer server;
    private final ObservationBook observations;
    private final ActionPayloadDecoder decoder = new ActionPayloadDecoder();
    private final StoryMutationMapper mapper = new StoryMutationMapper();
    private final NpcObservationFactory observationFactory = new NpcObservationFactory(decoder);
    private final NpcStartConflicts conflicts = new NpcStartConflicts();
    private final NpcRestActionStarter restActions;

    NpcActionStarter(MinecraftServer server, ObservationBook observations) {
        this.server = Objects.requireNonNull(server, "server");
        this.observations = Objects.requireNonNull(observations, "observations");
        this.restActions = new NpcRestActionStarter(observationFactory, conflicts, this.observations);
    }

    ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink sink) {
        return invoke(Invocation.from(request), sink, false);
    }

    ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink sink) {
        return invoke(Invocation.from(request), sink, true);
    }

    private ActionInvocationResult invoke(Invocation invocation, ActionCompletionSink sink, boolean recovery) {
        try {
            EchoNpcServiceLookupResult lookup = EchoNpcServiceBridge.lookup(server);
            if (lookup.status() != EchoNpcServiceLookupResult.Status.RESOLVED) {
                return recovery ? ActionInvocationResult.accepted() : rejected(lookup.message());
            }
            NpcService service = lookup.service().orElseThrow();
            ActorMutationId mutationId = mapper.mutationId(invocation.actionExecutionId());
            return route(invocation, sink, service, mutationId);
        } catch (IllegalArgumentException exception) {
            return rejected(exception.getMessage());
        }
    }

    private ActionInvocationResult route(Invocation invocation, ActionCompletionSink sink, NpcService service,
            ActorMutationId mutationId) {
        if (invocation.actionType().equals(NpcActionTypes.SPAWN_AT)) {
            return spawn(invocation, sink, service, mutationId, decoder.spawnAt(invocation.payload()));
        }
        if (invocation.actionType().equals(NpcActionTypes.SPAWN)) {
            return spawn(invocation, sink, service, mutationId, decoder.spawn(invocation.payload()));
        }
        if (invocation.actionType().equals(NpcActionTypes.TELEPORT)) {
            return teleport(invocation, sink, service, mutationId, decoder.teleport(invocation.payload()));
        }
        if (invocation.actionType().equals(NpcActionTypes.DESPAWN)) {
            return despawn(invocation, sink, service, mutationId, decoder.despawn(invocation.payload()));
        }
        if (invocation.actionType().equals(NpcActionTypes.SLEEP_AT)) {
            return restActions.sleepAt(invocation, sink, service, mutationId, decoder.sleepAt(invocation.payload()));
        }
        if (invocation.actionType().equals(NpcActionTypes.WAKE_UP)) {
            return restActions.wakeUp(invocation, sink, service, mutationId, decoder.wakeUp(invocation.payload()));
        }
        return rejected("Action type does not belong to Echo NPC adapter: " + invocation.actionType().asString());
    }

    private ActionInvocationResult spawn(
            Invocation invocation,
            ActionCompletionSink sink,
            NpcService service,
            ActorMutationId mutationId,
            SpawnPayload payload) {
        Optional<String> conflict = conflicts.existingMutation(service, mutationId,
                NpcResults.OperationKind.SPAWN, payload.actorId());
        if (conflict.isPresent()) {
            return rejected(conflict.orElseThrow());
        }
        Optional<String> actorConflict = conflicts.spawnActor(service, payload);
        if (actorConflict.isPresent()) {
            return rejected(actorConflict.orElseThrow());
        }
        NpcResults.Spawn result = service.spawn(new SpawnNpcRequest(payload.actorId(), payload.definitionId(),
                payload.worldId(), payload.position(), payload.rotation(), mutationId));
        if (result.status() == NpcResults.SpawnStatus.ACTOR_ID_CONFLICT) {
            return rejected(result.message());
        }
        if (result.status() == NpcResults.SpawnStatus.QUEUED || result.status() == NpcResults.SpawnStatus.CREATED) {
            return register(observationFactory.spawn(invocation, sink, mutationId, payload, "created"));
        }
        if (result.status() == NpcResults.SpawnStatus.ALREADY_ACTIVE) {
            if (result.actor().filter(actor -> service.isSleeping(actor.actorId())).isPresent()) {
                return observationFactory.completeSpawn(invocation, sink, payload, "already_present_sleeping");
            }
            if (result.actor().filter(actor -> !conflicts.matchesTransform(actor, payload)).isPresent()) {
                NpcResults.Teleport moved = service.teleport(new TeleportNpcRequest(payload.actorId(),
                        payload.worldId(), payload.position(), payload.rotation(), mutationId));
                if (moved.status() == NpcResults.TeleportStatus.QUEUED
                        || moved.status() == NpcResults.TeleportStatus.MOVED) {
                    return register(observationFactory.spawn(invocation, sink, mutationId, payload, "repositioned"));
                }
                return rejected(moved.message());
            }
            return register(observationFactory.spawn(invocation, sink, mutationId, payload, "already_present"));
        }
        return rejected(result.message());
    }

    private ActionInvocationResult teleport(
            Invocation invocation,
            ActionCompletionSink sink,
            NpcService service,
            ActorMutationId mutationId,
            TeleportPayload payload) {
        Optional<String> conflict = conflicts.existingMutation(service, mutationId,
                NpcResults.OperationKind.TELEPORT, payload.actorId());
        if (conflict.isPresent()) {
            return rejected(conflict.orElseThrow());
        }
        Optional<NpcActorView> actor = service.query(payload.actorId()).actor();
        if (actor.isEmpty()) {
            return rejected("Teleport rejected: ActorId is absent.");
        }
        if (conflicts.matchesTransform(actor.orElseThrow(), payload)) {
            return register(observationFactory.teleport(invocation, sink, mutationId, payload, "already_at_target"));
        }
        NpcResults.Teleport result = service.teleport(new TeleportNpcRequest(payload.actorId(), payload.worldId(),
                payload.position(), payload.rotation(), mutationId));
        if (result.status() == NpcResults.TeleportStatus.QUEUED || result.status() == NpcResults.TeleportStatus.MOVED) {
            return register(observationFactory.teleport(invocation, sink, mutationId, payload, "moved"));
        }
        return rejected(result.message());
    }

    private ActionInvocationResult despawn(
            Invocation invocation,
            ActionCompletionSink sink,
            NpcService service,
            ActorMutationId mutationId,
            DespawnPayload payload) {
        Optional<String> conflict = conflicts.existingMutation(service, mutationId,
                NpcResults.OperationKind.DESPAWN, payload.actorId());
        if (conflict.isPresent()) {
            return rejected(conflict.orElseThrow());
        }
        if (service.query(payload.actorId()).status() == NpcResults.QueryStatus.NOT_FOUND) {
            return register(observationFactory.despawn(invocation, sink, mutationId, payload, "already_absent"));
        }
        NpcResults.Despawn result = service.despawn(payload.actorId(), mutationId);
        if (result.status() == NpcResults.DespawnStatus.QUEUED || result.status() == NpcResults.DespawnStatus.REMOVED
                || result.status() == NpcResults.DespawnStatus.TOMBSTONED) {
            return register(observationFactory.despawn(invocation, sink, mutationId, payload, "removed"));
        }
        if (result.status() == NpcResults.DespawnStatus.ALREADY_ABSENT) {
            return register(observationFactory.despawn(invocation, sink, mutationId, payload, "already_absent"));
        }
        return rejected(result.message());
    }

    private ActionInvocationResult register(ActionObservation observation) {
        return switch (observations.register(observation)) {
            case ADDED, DUPLICATE -> ActionInvocationResult.accepted();
            case CONFLICT -> rejected("ActionExecutionId is already used with different arguments.");
            case FULL -> rejected("Adapter observation queue is full.");
        };
    }

    private ActionInvocationResult rejected(String detail) {
        return ActionInvocationResult.rejected("Echo Story NPC Adapter: " + detail);
    }

    record Invocation(
            StoryId storyId,
            ExecutionId executionId,
            ActionExecutionId actionExecutionId,
            NamespacedId actionType,
            Map<String, String> payload) {
        static Invocation from(ActionStartRequest request) {
            return new Invocation(request.storyId(), request.executionId(), request.actionExecutionId(),
                    request.actionType(), request.payload());
        }

        static Invocation from(ActionRecoveryRequest request) {
            return new Invocation(request.storyId(), request.executionId(), request.actionExecutionId(),
                    request.actionType(), request.payload());
        }
    }
}
