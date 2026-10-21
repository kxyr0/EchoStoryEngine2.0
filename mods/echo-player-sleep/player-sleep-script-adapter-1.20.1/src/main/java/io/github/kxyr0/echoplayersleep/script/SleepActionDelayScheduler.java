package io.github.kxyr0.echoplayersleep.script;

import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepResult;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import io.github.kxyr0.echoplayersleep.api.world.Ticks;
import io.github.kxyr0.echoplayersleep.forge.api.PlayerSleepServiceBridge;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public final class SleepActionDelayScheduler {
    private static final Object LOCK = new Object();
    private static final Map<DelayKey, Long> DURATIONS = new LinkedHashMap<>();
    private static final List<PendingWait> PENDING = new ArrayList<>();
    private static final List<PendingDelay> DELAYS = new ArrayList<>();
    private static final long CLOSED_DEFAULT_TICKS = 18L;
    private static final long OPEN_DEFAULT_TICKS = 16L;

    private SleepActionDelayScheduler() {
    }

    public static void rememberClose(String executionId, PlayerRef player, Ticks duration) {
        remember(new DelayKey(executionId, player.uuid().toString(), WaitKind.CLOSED), duration);
    }

    public static void rememberOpen(String executionId, PlayerRef player, Ticks duration) {
        remember(new DelayKey(executionId, player.uuid().toString(), WaitKind.OPEN), duration);
    }

    public static ActionInvocationResult waitUntilClosed(
            ActionStartRequest request,
            ActionCompletionSink sink,
            PlayerSleepServiceBridge services,
            SleepPayload payload,
            PlayerSleepCommandId command) {
        schedule(request, sink, services, payload, command, WaitKind.CLOSED, CLOSED_DEFAULT_TICKS);
        return ActionInvocationResult.accepted();
    }

    public static ActionInvocationResult waitUntilOpen(
            ActionStartRequest request,
            ActionCompletionSink sink,
            PlayerSleepServiceBridge services,
            SleepPayload payload,
            PlayerSleepCommandId command) {
        schedule(request, sink, services, payload, command, WaitKind.OPEN, OPEN_DEFAULT_TICKS);
        return ActionInvocationResult.accepted();
    }

    public static ActionInvocationResult wait(ActionStartRequest request, ActionCompletionSink sink, long ticks) {
        synchronized (LOCK) {
            DELAYS.add(new PendingDelay(request, sink, Math.max(1L, ticks)));
        }
        return ActionInvocationResult.accepted();
    }

    public static void tick() {
        List<PendingWait> due = new ArrayList<>();
        List<PendingDelay> delays = new ArrayList<>();
        synchronized (LOCK) {
            ListIterator<PendingWait> iterator = PENDING.listIterator();
            while (iterator.hasNext()) {
                PendingWait next = iterator.next().tick();
                if (next.remainingTicks() <= 0L) {
                    iterator.remove();
                    due.add(next);
                } else {
                    iterator.set(next);
                }
            }
            ListIterator<PendingDelay> delayIterator = DELAYS.listIterator();
            while (delayIterator.hasNext()) {
                PendingDelay next = delayIterator.next().tick();
                if (next.remainingTicks() <= 0L) {
                    delayIterator.remove();
                    delays.add(next);
                } else {
                    delayIterator.set(next);
                }
            }
        }
        due.forEach(PendingWait::complete);
        delays.forEach(PendingDelay::complete);
    }

    public static void clear() {
        synchronized (LOCK) {
            DURATIONS.clear();
            PENDING.clear();
            DELAYS.clear();
        }
    }

    private static void remember(DelayKey key, Ticks duration) {
        synchronized (LOCK) {
            DURATIONS.put(key, Math.max(1L, duration.value()));
        }
    }

    private static void schedule(
            ActionStartRequest request,
            ActionCompletionSink sink,
            PlayerSleepServiceBridge services,
            SleepPayload payload,
            PlayerSleepCommandId command,
            WaitKind kind,
            long fallbackTicks) {
        long delay = consumeDelay(request.executionId().asString(), payload.player(), kind, fallbackTicks);
        synchronized (LOCK) {
            PENDING.add(new PendingWait(request, sink, services, payload, command, kind, delay));
        }
    }

    private static long consumeDelay(String executionId, PlayerRef player, WaitKind kind, long fallbackTicks) {
        DelayKey key = new DelayKey(executionId, player.uuid().toString(), kind);
        synchronized (LOCK) {
            Long delay = DURATIONS.remove(key);
            return delay == null ? fallbackTicks : Math.max(1L, delay);
        }
    }

    private enum WaitKind {
        CLOSED {
            @Override
            PlayerSleepResult complete(PlayerSleepServiceBridge services, PlayerRef player,
                                       PlayerSleepCommandId command) {
                return services.sessions().waitUntilClosed(player, command);
            }
        },
        OPEN {
            @Override
            PlayerSleepResult complete(PlayerSleepServiceBridge services, PlayerRef player,
                                       PlayerSleepCommandId command) {
                return services.sessions().waitUntilOpen(player, command);
            }
        };

        abstract PlayerSleepResult complete(PlayerSleepServiceBridge services, PlayerRef player,
                                            PlayerSleepCommandId command);
    }

    private record DelayKey(String executionId, String player, WaitKind kind) {
    }

    private record PendingWait(
            ActionStartRequest request,
            ActionCompletionSink sink,
            PlayerSleepServiceBridge services,
            SleepPayload payload,
            PlayerSleepCommandId command,
            WaitKind kind,
            long remainingTicks) {
        PendingWait tick() {
            return new PendingWait(request, sink, services, payload, command, kind, remainingTicks - 1L);
        }

        void complete() {
            PlayerSleepResult result = kind.complete(services, payload.player(), command);
            sink.complete(new ActionCompletion(request.storyId(), request.executionId(),
                    request.actionExecutionId(), actionType(request), category(result),
                    Map.of("outcome", result.outcome().name())));
        }

        private NamespacedId actionType(ActionStartRequest request) {
            return request.actionType();
        }

        private ExternalActionResultCategory category(PlayerSleepResult result) {
            return result.diagnostics().isEmpty()
                    ? ExternalActionResultCategory.SUCCESS
                    : ExternalActionResultCategory.FAILURE;
        }
    }

    private record PendingDelay(
            ActionStartRequest request,
            ActionCompletionSink sink,
            long remainingTicks) {
        PendingDelay tick() {
            return new PendingDelay(request, sink, remainingTicks - 1L);
        }

        void complete() {
            sink.complete(new ActionCompletion(request.storyId(), request.executionId(),
                    request.actionExecutionId(), request.actionType(), ExternalActionResultCategory.SUCCESS,
                    Map.of("outcome", "COMPLETED")));
        }
    }
}
