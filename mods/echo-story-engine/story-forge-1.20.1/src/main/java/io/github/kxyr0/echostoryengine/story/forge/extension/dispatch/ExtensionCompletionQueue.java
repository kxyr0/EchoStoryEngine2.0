package io.github.kxyr0.echostoryengine.story.forge.extension.dispatch;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionAcceptance;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Thread-safe bounded queue that returns external completions to the server thread.
 */
public final class ExtensionCompletionQueue {
    private final int capacity;
    private final Queue<ActionCompletion> completions = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, TrackedAction> tracked = new ConcurrentHashMap<>();
    private final Set<String> completed = ConcurrentHashMap.newKeySet();
    private final AtomicInteger pending = new AtomicInteger();
    private final AtomicBoolean accepting = new AtomicBoolean(true);

    public ExtensionCompletionQueue(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative");
        }
        this.capacity = capacity;
    }

    public ActionCompletionSink sink() {
        return this::complete;
    }

    public void track(StoryExecutionState state, ExternalCommand command) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(command, "command");
        String key = command.actionExecutionId().asString();
        tracked.putIfAbsent(key, new TrackedAction(state.storyId(), state.executionId(),
                command.actionExecutionId(), command.actionType()));
    }

    public ActionCompletionAcceptance complete(ActionCompletion completion) {
        ActionCompletion checked = Objects.requireNonNull(completion, "completion");
        if (!accepting.get()) {
            return ActionCompletionAcceptance.SHUTDOWN;
        }
        String key = checked.actionExecutionId().asString();
        TrackedAction action = tracked.get(key);
        if (action == null) {
            return completed.contains(key)
                    ? ActionCompletionAcceptance.DUPLICATE
                    : ActionCompletionAcceptance.UNKNOWN_ACTION;
        }
        if (!action.matches(checked.storyId(), checked.executionId(), checked.actionType())) {
            return ActionCompletionAcceptance.INVALID_REQUEST;
        }
        if (!reserveSlot()) {
            return ActionCompletionAcceptance.QUEUE_FULL;
        }
        if (!completed.add(key)) {
            pending.decrementAndGet();
            return ActionCompletionAcceptance.DUPLICATE;
        }
        completions.add(checked);
        return ActionCompletionAcceptance.ACCEPTED;
    }

    public int drain(int maxResults, Consumer<ActionCompletion> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int drained = 0;
        while (drained < maxResults) {
            ActionCompletion completion = completions.poll();
            if (completion == null) {
                return drained;
            }
            tracked.remove(completion.actionExecutionId().asString());
            pending.decrementAndGet();
            consumer.accept(completion);
            drained++;
        }
        return drained;
    }

    public void shutdown() {
        accepting.set(false);
        tracked.clear();
    }

    public int pendingCount() {
        return pending.get();
    }

    public int capacity() {
        return capacity;
    }

    private boolean reserveSlot() {
        while (accepting.get()) {
            int current = pending.get();
            if (current >= capacity) {
                return false;
            }
            if (pending.compareAndSet(current, current + 1)) {
                return true;
            }
        }
        return false;
    }
}
