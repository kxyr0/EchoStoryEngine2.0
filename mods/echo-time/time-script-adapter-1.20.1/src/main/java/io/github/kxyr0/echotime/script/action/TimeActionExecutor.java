package io.github.kxyr0.echotime.script.action;

import io.github.kxyr0.echotime.api.mutation.TimeAddRequest;
import io.github.kxyr0.echotime.api.mutation.TimeCrossingPolicy;
import io.github.kxyr0.echotime.api.mutation.TimeMutationResult;
import io.github.kxyr0.echotime.api.mutation.TimeMutationStatus;
import io.github.kxyr0.echotime.api.mutation.TimeSetRequest;
import io.github.kxyr0.echotime.api.wait.TimeWaitStatus;
import io.github.kxyr0.echotime.core.clock.TimePredicates;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceBridge;
import java.util.Map;

public final class TimeActionExecutor {
    private final EchoTimeServiceBridge services;
    private final TimePredicates predicates = new TimePredicates();

    public TimeActionExecutor(EchoTimeServiceBridge services) { this.services = services; }

    public TimeActionResult execute(String type, Map<String, String> payload, TimeActionContext context) {
        try {
            TimeActionArguments arguments = new TimeActionArguments(payload);
            return switch (path(type)) {
                case "set" -> mutation(services.mutations().set(new TimeSetRequest(context.mutationId(),
                        arguments.day(), arguments.time("time"),
                        arguments.crossing(TimeCrossingPolicy.SKIP_CROSSED), context.source())));
                case "add" -> mutation(services.mutations().add(new TimeAddRequest(context.mutationId(),
                        arguments.duration(), arguments.crossing(TimeCrossingPolicy.FIRE_CROSSED), context.source())));
                case "pause" -> success(services.mutations().pause(context.pauseOwner(), context.mutationId()).status());
                case "resume" -> success(services.mutations().resume(context.pauseOwner(), context.mutationId()).status());
                case "wait_until" -> waitFor(arguments, context);
                case "is" -> predicate(predicates.is(current(), arguments.time("time")));
                case "is_before" -> predicate(predicates.isBefore(current(), arguments.time("time")));
                case "is_after" -> predicate(predicates.isAfter(current(), arguments.time("time")));
                case "is_between" -> predicate(predicates.isBetween(
                        current(), arguments.time("start"), arguments.time("end")));
                default -> TimeActionResult.failure("Unsupported Time action type: " + type);
            };
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return TimeActionResult.failure(exception.getMessage());
        }
    }

    private TimeActionResult mutation(TimeMutationResult result) {
        boolean accepted = result.status() == TimeMutationStatus.APPLIED
                || result.status() == TimeMutationStatus.DUPLICATE;
        return accepted ? TimeActionResult.success(result.message(), Map.of(
                "day", Long.toString(result.current().day().value()),
                "time", result.current().timeOfDay().formatHHmm(),
                "revision", Long.toString(result.current().revision().value())))
                : TimeActionResult.failure(result.status() + ": " + result.message());
    }

    private TimeActionResult waitFor(TimeActionArguments arguments, TimeActionContext context) {
        var result = services.waits().waitUntil(context.waitId(), arguments.time("time"));
        if (result.status() == TimeWaitStatus.IDEMPOTENCY_CONFLICT) {
            return TimeActionResult.failure("Time wait idempotency conflict.");
        }
        if (result.status() == TimeWaitStatus.COMPLETE) {
            services.waits().complete(context.waitId());
            return TimeActionResult.success("wait already complete", Map.of());
        }
        return TimeActionResult.waiting(context.waitId());
    }

    private TimeActionResult predicate(boolean value) {
        return TimeActionResult.success("predicate", Map.of("value", Boolean.toString(value)));
    }

    private TimeActionResult success(Object value) {
        return TimeActionResult.success(value.toString(), Map.of());
    }

    private io.github.kxyr0.echotime.api.value.TimeOfDay current() {
        return services.query().current().instant().timeOfDay();
    }

    private String path(String type) {
        int separator = type.indexOf(':');
        return separator < 0 ? type : type.substring(separator + 1);
    }
}
