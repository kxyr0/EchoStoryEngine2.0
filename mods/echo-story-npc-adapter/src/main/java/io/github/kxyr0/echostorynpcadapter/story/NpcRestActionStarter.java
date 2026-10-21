package io.github.kxyr0.echostorynpcadapter.story;

import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.rest.command.NpcSleepRequest;
import io.github.kxyr0.echonpc.api.rest.command.NpcWakeRequest;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import io.github.kxyr0.echostorynpcadapter.action.SleepPayload;
import io.github.kxyr0.echostorynpcadapter.action.WakePayload;
import io.github.kxyr0.echostorynpcadapter.observe.ActionObservation;
import io.github.kxyr0.echostorynpcadapter.observe.ObservationBook;
import io.github.kxyr0.echostorynpcadapter.story.conflict.NpcStartConflicts;
import java.util.Optional;

final class NpcRestActionStarter {
    private final NpcObservationFactory observationFactory;
    private final NpcStartConflicts conflicts;
    private final ObservationBook observations;

    NpcRestActionStarter(
            NpcObservationFactory observationFactory,
            NpcStartConflicts conflicts,
            ObservationBook observations) {
        this.observationFactory = java.util.Objects.requireNonNull(observationFactory, "observationFactory");
        this.conflicts = java.util.Objects.requireNonNull(conflicts, "conflicts");
        this.observations = java.util.Objects.requireNonNull(observations, "observations");
    }

    ActionInvocationResult sleepAt(
            NpcActionStarter.Invocation invocation,
            ActionCompletionSink sink,
            NpcService service,
            ActorMutationId mutationId,
            SleepPayload payload) {
        Optional<String> conflict = conflicts.existingMutation(service, mutationId,
                NpcResults.OperationKind.SLEEP, payload.actorId());
        if (conflict.isPresent()) {
            return rejected(conflict.orElseThrow());
        }
        NpcResults.Sleep result = service.sleepAt(new NpcSleepRequest(payload.actorId(), payload.worldId(),
                payload.bedPosition(), payload.expectedBlock(), mutationId));
        if (result.status() == NpcResults.SleepStatus.QUEUED || result.status() == NpcResults.SleepStatus.SLEEPING) {
            return register(observationFactory.sleep(invocation, sink, mutationId, payload, "sleeping"));
        }
        if (result.status() == NpcResults.SleepStatus.ALREADY_SLEEPING) {
            return register(observationFactory.sleep(invocation, sink, mutationId, payload, "already_sleeping"));
        }
        return rejected(result.message());
    }

    ActionInvocationResult wakeUp(
            NpcActionStarter.Invocation invocation,
            ActionCompletionSink sink,
            NpcService service,
            ActorMutationId mutationId,
            WakePayload payload) {
        Optional<String> conflict = conflicts.existingMutation(service, mutationId,
                NpcResults.OperationKind.WAKE, payload.actorId());
        if (conflict.isPresent()) {
            return rejected(conflict.orElseThrow());
        }
        NpcResults.Wake result = service.wakeUp(new NpcWakeRequest(payload.actorId(), mutationId));
        if (result.status() == NpcResults.WakeStatus.QUEUED || result.status() == NpcResults.WakeStatus.WOKE_UP) {
            return register(observationFactory.wake(invocation, sink, mutationId, payload, "woke_up"));
        }
        if (result.status() == NpcResults.WakeStatus.ALREADY_AWAKE) {
            return register(observationFactory.wake(invocation, sink, mutationId, payload, "already_awake"));
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
}
