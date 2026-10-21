package io.github.kxyr0.echostorynpcadapter.story;

import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostorynpcadapter.action.ActionPayloadDecoder;
import io.github.kxyr0.echostorynpcadapter.action.DespawnPayload;
import io.github.kxyr0.echostorynpcadapter.action.SleepPayload;
import io.github.kxyr0.echostorynpcadapter.action.SpawnPayload;
import io.github.kxyr0.echostorynpcadapter.action.TeleportPayload;
import io.github.kxyr0.echostorynpcadapter.action.WakePayload;
import io.github.kxyr0.echostorynpcadapter.observe.ActionObservation;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.Map;
import java.util.Optional;

final class NpcObservationFactory {
    private final ActionPayloadDecoder decoder;

    NpcObservationFactory(ActionPayloadDecoder decoder) {
        this.decoder = java.util.Objects.requireNonNull(decoder, "decoder");
    }

    ActionObservation spawn(
            NpcActionStarter.Invocation invocation,
            ActionCompletionSink sink,
            ActorMutationId mutationId,
            SpawnPayload payload,
            String result) {
        return new ActionObservation(ActionObservation.Kind.SPAWN, invocation.storyId(), invocation.executionId(),
                invocation.actionExecutionId(), invocation.actionType(), mutationId, payload.actorId(),
                Optional.of(payload.definitionId()), Optional.of(payload.worldId()), Optional.of(payload.position()),
                Optional.of(payload.rotation()), Optional.empty(), Optional.empty(), result,
                decoder.fingerprint(invocation.payload()), sink);
    }

    ActionInvocationResult completeSpawn(
            NpcActionStarter.Invocation invocation,
            ActionCompletionSink sink,
            SpawnPayload payload,
            String result) {
        sink.complete(new ActionCompletion(invocation.storyId(), invocation.executionId(),
                invocation.actionExecutionId(), invocation.actionType(), ExternalActionResultCategory.SUCCESS,
                Map.of("actorId", payload.actorId().value(), "result", result)));
        return ActionInvocationResult.accepted();
    }

    ActionObservation teleport(
            NpcActionStarter.Invocation invocation,
            ActionCompletionSink sink,
            ActorMutationId mutationId,
            TeleportPayload payload,
            String result) {
        return new ActionObservation(ActionObservation.Kind.TELEPORT, invocation.storyId(), invocation.executionId(),
                invocation.actionExecutionId(), invocation.actionType(), mutationId, payload.actorId(),
                Optional.empty(), Optional.of(payload.worldId()), Optional.of(payload.position()),
                Optional.of(payload.rotation()), Optional.empty(), Optional.empty(), result,
                decoder.fingerprint(invocation.payload()), sink);
    }

    ActionObservation despawn(
            NpcActionStarter.Invocation invocation,
            ActionCompletionSink sink,
            ActorMutationId mutationId,
            DespawnPayload payload,
            String result) {
        return new ActionObservation(ActionObservation.Kind.DESPAWN, invocation.storyId(), invocation.executionId(),
                invocation.actionExecutionId(), invocation.actionType(), mutationId, payload.actorId(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), result,
                decoder.fingerprint(invocation.payload()), sink);
    }

    ActionObservation sleep(
            NpcActionStarter.Invocation invocation,
            ActionCompletionSink sink,
            ActorMutationId mutationId,
            SleepPayload payload,
            String result) {
        return new ActionObservation(ActionObservation.Kind.SLEEP, invocation.storyId(), invocation.executionId(),
                invocation.actionExecutionId(), invocation.actionType(), mutationId, payload.actorId(),
                Optional.empty(), Optional.of(payload.worldId()), Optional.empty(), Optional.empty(),
                Optional.of(payload.bedPosition()), Optional.of(payload.expectedBlock()), result,
                decoder.fingerprint(invocation.payload()), sink);
    }

    ActionObservation wake(
            NpcActionStarter.Invocation invocation,
            ActionCompletionSink sink,
            ActorMutationId mutationId,
            WakePayload payload,
            String result) {
        return new ActionObservation(ActionObservation.Kind.WAKE, invocation.storyId(), invocation.executionId(),
                invocation.actionExecutionId(), invocation.actionType(), mutationId, payload.actorId(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), result, decoder.fingerprint(invocation.payload()), sink);
    }
}
