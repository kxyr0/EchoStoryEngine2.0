package io.github.kxyr0.echoscene.forge.command;

import io.github.kxyr0.echoscene.api.command.SceneCommandRequest;
import io.github.kxyr0.echoscene.api.command.SceneCommandStart;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import io.github.kxyr0.echotime.api.mutation.TimeAddRequest;
import io.github.kxyr0.echotime.api.mutation.TimeCrossingPolicy;
import io.github.kxyr0.echotime.api.mutation.TimeMutationStatus;
import io.github.kxyr0.echotime.api.mutation.TimeRequestSource;
import io.github.kxyr0.echotime.api.mutation.TimeSetRequest;
import io.github.kxyr0.echotime.api.value.EchoDuration;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.api.wait.TimeWaitStatus;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceAccess;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceBridge;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;

final class TimeCommandAdapter {
    private final Function<SceneExecutionId, ServerPlayer> players;
    private final Map<String, Pending> waits = new LinkedHashMap<>();

    TimeCommandAdapter(Function<SceneExecutionId, ServerPlayer> players) { this.players = players; }

    SceneCommandStart start(SceneCommandRequest request) {
        return switch (request.command().operation()) {
            case "Set" -> set(request);
            case "Add" -> add(request);
            case "WaitUntil" -> waitUntil(request);
            default -> SceneCommandStart.failed("unsupported Echo Time operation");
        };
    }

    void tick(SceneCommandHub.Completion completion) {
        for (Pending pending : List.copyOf(waits.values())) {
            service(pending.player()).ifPresent(services -> {
                var result = services.waits().waitUntil(pending.waitId(), pending.time());
                if (result.status() == TimeWaitStatus.COMPLETE) finish(pending, true, "", completion, services);
                if (result.status() == TimeWaitStatus.IDEMPOTENCY_CONFLICT) {
                    finish(pending, false, "Echo Time wait idempotency conflict", completion, services);
                }
            });
        }
    }

    void cancelOwned(SceneExecutionId owner) {
        waits.values().removeIf(value -> {
            if (!value.execution().equals(owner)) return false;
            service(value.player()).ifPresent(services -> services.waits().complete(value.waitId()));
            return true;
        });
    }

    private SceneCommandStart set(SceneCommandRequest request) {
        EchoTimeServiceBridge services = service(request).orElse(null);
        if (services == null) return SceneCommandStart.failed("Echo Time service unavailable");
        var result = services.mutations().set(new TimeSetRequest(new TimeMutationId(SceneCommandHub.key(request)),
                Optional.empty(), TimeOfDay.parse(argument(request)), TimeCrossingPolicy.SKIP_CROSSED,
                source(request)));
        return mutation(result.status(), result.message());
    }

    private SceneCommandStart add(SceneCommandRequest request) {
        EchoTimeServiceBridge services = service(request).orElse(null);
        if (services == null) return SceneCommandStart.failed("Echo Time service unavailable");
        var result = services.mutations().add(new TimeAddRequest(new TimeMutationId(SceneCommandHub.key(request)),
                duration(argument(request)), TimeCrossingPolicy.SKIP_CROSSED, source(request)));
        return mutation(result.status(), result.message());
    }

    private SceneCommandStart waitUntil(SceneCommandRequest request) {
        EchoTimeServiceBridge services = service(request).orElse(null);
        ServerPlayer player = players.apply(request.executionId());
        if (services == null || player == null) return SceneCommandStart.failed("Echo Time service unavailable");
        String key = SceneCommandHub.key(request);
        Pending pending = new Pending(key, request.executionId(), request.command().stepId(),
                new TimeWaitId(key), TimeOfDay.parse(argument(request)), player);
        var result = services.waits().waitUntil(pending.waitId(), pending.time());
        if (result.status() == TimeWaitStatus.COMPLETE) return SceneCommandStart.completed();
        if (result.status() == TimeWaitStatus.IDEMPOTENCY_CONFLICT) {
            return SceneCommandStart.failed("Echo Time wait idempotency conflict");
        }
        waits.put(key, pending);
        return SceneCommandStart.waiting(key);
    }

    private void finish(Pending pending, boolean success, String detail, SceneCommandHub.Completion completion,
            EchoTimeServiceBridge services) {
        services.waits().complete(pending.waitId());
        waits.remove(pending.key());
        completion.complete(pending.execution(), pending.step(), success, detail);
    }

    private Optional<EchoTimeServiceBridge> service(SceneCommandRequest request) {
        return service(players.apply(request.executionId()));
    }

    private Optional<EchoTimeServiceBridge> service(ServerPlayer player) {
        return player == null ? Optional.empty() : EchoTimeServiceAccess.find(player.getServer());
    }

    private String argument(SceneCommandRequest request) { return request.command().arguments().get(0); }
    private SceneCommandStart mutation(TimeMutationStatus status, String detail) {
        return status == TimeMutationStatus.APPLIED || status == TimeMutationStatus.DUPLICATE
                ? SceneCommandStart.completed() : SceneCommandStart.failed(detail);
    }
    private EchoDuration duration(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith("h")) {
            return EchoDuration.hours(Long.parseLong(normalized.substring(0, normalized.length() - 1)));
        }
        String minutes = normalized.endsWith("m") ? normalized.substring(0, normalized.length() - 1) : normalized;
        return EchoDuration.minutes(Long.parseLong(minutes));
    }
    private TimeRequestSource source(SceneCommandRequest request) {
        return new TimeRequestSource("echo_scene", "scene", request.executionId().value(), request.rootOwner(),
                Optional.of(request.resourceId().value()), Optional.of(request.command().span().start().line()),
                "Scene " + request.sceneId().value());
    }

    private record Pending(String key, SceneExecutionId execution, SceneStepId step, TimeWaitId waitId,
                           TimeOfDay time, ServerPlayer player) { }
}
