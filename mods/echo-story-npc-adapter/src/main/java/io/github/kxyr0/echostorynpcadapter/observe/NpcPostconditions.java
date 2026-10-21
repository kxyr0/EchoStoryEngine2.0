package io.github.kxyr0.echostorynpcadapter.observe;

import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import java.util.Map;
import java.util.Optional;

/**
 * Verifies terminal state through public Echo NPC read models only.
 */
public final class NpcPostconditions {
    public Optional<ActionCompletion> completion(ActionObservation observation, NpcService service) {
        return switch (observation.kind()) {
            case SPAWN -> spawn(observation, service);
            case TELEPORT -> teleport(observation, service);
            case DESPAWN -> despawn(observation, service);
            case SLEEP -> sleep(observation, service);
            case WAKE -> wake(observation, service);
        };
    }

    private Optional<ActionCompletion> spawn(ActionObservation observation, NpcService service) {
        return terminalFailure(observation, service).or(() -> service.query(observation.actorId()).actor()
                .filter(actor -> materialized(actor) && actor.definitionId().equals(
                        observation.definitionId().orElseThrow()))
                .filter(actor -> matchesTransform(observation, actor))
                .map(actor -> success(observation)));
    }

    private Optional<ActionCompletion> teleport(ActionObservation observation, NpcService service) {
        return terminalFailure(observation, service).or(() -> service.query(observation.actorId()).actor()
                .filter(this::materialized)
                .filter(actor -> matchesTransform(observation, actor))
                .map(actor -> success(observation)));
    }

    private Optional<ActionCompletion> despawn(ActionObservation observation, NpcService service) {
        if (service.query(observation.actorId()).status() == NpcResults.QueryStatus.NOT_FOUND) {
            return Optional.of(success(observation));
        }
        return terminalFailure(observation, service);
    }

    private Optional<ActionCompletion> sleep(ActionObservation observation, NpcService service) {
        return terminalFailure(observation, service).or(() -> service.query(observation.actorId()).actor()
                .filter(actor -> service.isSleepingAt(actor.actorId(), observation.bedPosition().orElseThrow()))
                .map(actor -> success(observation)));
    }

    private Optional<ActionCompletion> wake(ActionObservation observation, NpcService service) {
        return terminalFailure(observation, service).or(() -> service.query(observation.actorId()).actor()
                .filter(actor -> !service.isSleeping(actor.actorId()))
                .map(actor -> success(observation)));
    }

    private Optional<ActionCompletion> terminalFailure(ActionObservation observation, NpcService service) {
        return service.operation(observation.mutationId().value()).operation()
                .filter(operation -> operation.status() == NpcResults.OperationStatus.FAILED
                        || operation.status() == NpcResults.OperationStatus.REJECTED)
                .map(operation -> failure(observation));
    }

    private boolean materialized(NpcActorView actor) {
        return actor.status() == NpcActorView.Status.MATERIALIZED
                && actor.desiredPresence() == NpcActorView.DesiredPresence.ACTIVE
                && actor.definitionCompatibility() == NpcActorView.DefinitionCompatibility.COMPATIBLE
                && actor.materializedPosition().isPresent();
    }

    private boolean matchesTransform(ActionObservation observation, NpcActorView actor) {
        return actor.worldId().equals(observation.worldId().orElseThrow())
                && actor.position().equals(observation.position().orElseThrow())
                && actor.rotation().equals(observation.rotation().orElseThrow());
    }

    private ActionCompletion success(ActionObservation observation) {
        return new ActionCompletion(observation.storyId(), observation.executionId(), observation.actionExecutionId(),
                observation.actionType(), ExternalActionResultCategory.SUCCESS,
                Map.of("actorId", observation.actorId().value(), "result", observation.successResult()));
    }

    private ActionCompletion failure(ActionObservation observation) {
        return new ActionCompletion(observation.storyId(), observation.executionId(), observation.actionExecutionId(),
                observation.actionType(), ExternalActionResultCategory.FAILURE,
                Map.of("actorId", observation.actorId().value()));
    }
}
