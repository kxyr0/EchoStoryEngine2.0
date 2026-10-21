package io.github.kxyr0.echotrigger.core.definition;

import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.policy.TriggerManualFirePolicy;
import io.github.kxyr0.echotrigger.api.policy.TriggerMode;
import io.github.kxyr0.echotrigger.api.policy.TriggerScope;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CompiledTriggerDefinition(
        TriggerId id,
        TriggerScope scope,
        TriggerMode mode,
        TriggerRevision revision,
        long cooldownTicks,
        TriggerManualFirePolicy manualFire,
        List<TriggerSequenceStep> sequence,
        Optional<ReactiveTriggerDefinition> reactive,
        Optional<TriggerStorySubscription> storySubscription,
        String fingerprint,
        Optional<io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition> eventSubscription
) {
    public CompiledTriggerDefinition(TriggerId id, TriggerScope scope, TriggerMode mode, TriggerRevision revision,
            long cooldownTicks, TriggerManualFirePolicy manualFire, List<TriggerSequenceStep> sequence,
            Optional<ReactiveTriggerDefinition> reactive, Optional<TriggerStorySubscription> storySubscription,
            String fingerprint) {
        this(id, scope, mode, revision, cooldownTicks, manualFire, sequence, reactive, storySubscription,
                fingerprint, Optional.empty());
    }

    public CompiledTriggerDefinition(
            TriggerId id,
            TriggerScope scope,
            TriggerMode mode,
            TriggerRevision revision,
            long cooldownTicks,
            TriggerManualFirePolicy manualFire,
            List<TriggerSequenceStep> sequence,
            String fingerprint
    ) {
        this(id, scope, mode, revision, cooldownTicks, manualFire, sequence,
                Optional.empty(), Optional.empty(), fingerprint);
    }

    public CompiledTriggerDefinition(
            TriggerId id,
            TriggerScope scope,
            TriggerMode mode,
            TriggerRevision revision,
            long cooldownTicks,
            TriggerManualFirePolicy manualFire,
            List<TriggerSequenceStep> sequence,
            Optional<ReactiveTriggerDefinition> reactive,
            String fingerprint
    ) {
        this(id, scope, mode, revision, cooldownTicks, manualFire, sequence,
                reactive, Optional.empty(), fingerprint);
    }

    public CompiledTriggerDefinition {
        id = Objects.requireNonNull(id, "id");
        scope = Objects.requireNonNull(scope, "scope");
        mode = Objects.requireNonNull(mode, "mode");
        revision = Objects.requireNonNull(revision, "revision");
        manualFire = Objects.requireNonNull(manualFire, "manualFire");
        sequence = List.copyOf(Objects.requireNonNull(sequence, "sequence"));
        reactive = Objects.requireNonNull(reactive, "reactive");
        storySubscription = Objects.requireNonNull(storySubscription, "storySubscription");
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        if (cooldownTicks < 0) {
            throw new IllegalArgumentException("cooldownTicks must not be negative.");
        }
        eventSubscription = Objects.requireNonNull(eventSubscription, "eventSubscription");
        if (sequence.isEmpty() && reactive.isEmpty() && storySubscription.isEmpty() && eventSubscription.isEmpty()) {
            throw new IllegalArgumentException(
                    "Trigger definition requires sequence, subscription, or reactive callbacks.");
        }
    }
}
