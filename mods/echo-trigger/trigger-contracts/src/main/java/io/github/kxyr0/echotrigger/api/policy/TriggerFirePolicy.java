package io.github.kxyr0.echotrigger.api.policy;

import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackKind;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

public record TriggerFirePolicy(
        boolean fireOnlyOnce,
        Set<TriggerCallbackKind> oneShotCallbacks,
        OptionalInt maxFireCount,
        long cooldownTicks
) {
    public TriggerFirePolicy {
        oneShotCallbacks = Set.copyOf(Objects.requireNonNull(oneShotCallbacks, "oneShotCallbacks"));
        maxFireCount = Objects.requireNonNull(maxFireCount, "maxFireCount");
        if (maxFireCount.isPresent() && maxFireCount.orElseThrow() <= 0) {
            throw new IllegalArgumentException("maxFireCount must be positive.");
        }
        if (cooldownTicks < 0) {
            throw new IllegalArgumentException("cooldownTicks must not be negative.");
        }
    }

    public static TriggerFirePolicy repeat(long cooldownTicks) {
        return new TriggerFirePolicy(false, Set.of(), OptionalInt.empty(), cooldownTicks);
    }

    public boolean onceFor(TriggerCallbackKind kind) {
        return fireOnlyOnce || oneShotCallbacks.contains(kind);
    }
}
