package io.github.kxyr0.echoobservation.forge.story;

import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import io.github.kxyr0.echoobservation.api.runtime.ObservationService;
import io.github.kxyr0.echoobservation.api.runtime.ObservationServiceResult;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

record ObservationStoryOperation(NamespacedId actionType, Executor executor) {
    static List<ObservationStoryOperation> all() {
        return List.of(
                resource("activate", ObservationService::activate),
                resource("deactivate", ObservationService::deactivate),
                point("enable", ObservationService::enable),
                point("disable", ObservationService::disable),
                queryPoint("is_completed", ObservationService::isCompleted),
                queryResource("is_active", ObservationService::isActive));
    }

    private static ObservationStoryOperation resource(String name, ResourceMutation mutation) {
        return operation(name, (service, player, target) -> result(mutation.apply(service, player,
                ObservationResourceId.parse(target))));
    }

    private static ObservationStoryOperation point(String name, PointMutation mutation) {
        return operation(name, (service, player, target) -> result(mutation.apply(service, player,
                ObservationId.parse(target))));
    }

    private static ObservationStoryOperation queryPoint(String name, PointQuery query) {
        return operation(name, (service, player, target) -> Result.success(Boolean.toString(
                query.apply(service, player, ObservationId.parse(target)))));
    }

    private static ObservationStoryOperation queryResource(String name, ResourceQuery query) {
        return operation(name, (service, player, target) -> Result.success(Boolean.toString(
                query.apply(service, player, ObservationResourceId.parse(target)))));
    }

    private static ObservationStoryOperation operation(String name, Executor executor) {
        return new ObservationStoryOperation(NamespacedId.of("echo_observation:" + name), executor);
    }

    private static Result result(ObservationServiceResult value) {
        return value.successful() ? Result.success("")
                : Result.failure(value.error().map(Enum::name).orElse("OBSERVATION_ACTION_FAILED"));
    }

    Result execute(ObservationService service, UUID player, String target) {
        try { return executor.execute(service, player, target); }
        catch (IllegalArgumentException exception) { return Result.failure("OBSERVATION_TARGET_INVALID"); }
    }

    interface Executor { Result execute(ObservationService service, UUID player, String target); }
    interface ResourceMutation { ObservationServiceResult apply(ObservationService s, UUID p, ObservationResourceId i); }
    interface PointMutation { ObservationServiceResult apply(ObservationService s, UUID p, ObservationId i); }
    interface ResourceQuery { boolean apply(ObservationService s, UUID p, ObservationResourceId i); }
    interface PointQuery { boolean apply(ObservationService s, UUID p, ObservationId i); }
    record Result(boolean successful, Optional<String> value, String code) {
        static Result success(String value) { return new Result(true,
                value.isEmpty() ? Optional.empty() : Optional.of(value), ""); }
        static Result failure(String code) { return new Result(false, Optional.empty(), code); }
    }
}
