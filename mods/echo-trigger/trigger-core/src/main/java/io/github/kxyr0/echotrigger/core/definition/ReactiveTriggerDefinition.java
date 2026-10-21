package io.github.kxyr0.echotrigger.core.definition;

import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackDefinition;
import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackKind;
import io.github.kxyr0.echotrigger.api.policy.TriggerFirePolicy;
import io.github.kxyr0.echotrigger.api.policy.TriggerStayPolicy;
import java.util.Map;
import java.util.Objects;

public record ReactiveTriggerDefinition(
        TriggerBoundArea primaryArea,
        Map<TriggerCallbackKind, TriggerCallbackDefinition> callbacks,
        TriggerFirePolicy firePolicy,
        long stayIntervalTicks,
        TriggerStayPolicy stayPolicy
) {
    public ReactiveTriggerDefinition {
        primaryArea = Objects.requireNonNull(primaryArea, "primaryArea");
        callbacks = Map.copyOf(Objects.requireNonNull(callbacks, "callbacks"));
        firePolicy = Objects.requireNonNull(firePolicy, "firePolicy");
        stayPolicy = Objects.requireNonNull(stayPolicy, "stayPolicy");
        if (callbacks.isEmpty()) {
            throw new IllegalArgumentException("Reactive trigger requires at least one callback.");
        }
        if (stayIntervalTicks <= 0) {
            throw new IllegalArgumentException("stayIntervalTicks must be positive.");
        }
    }
}
