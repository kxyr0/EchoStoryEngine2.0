package io.github.kxyr0.echotriggerplayersleepadapter.forge;

import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SleepSubscriptionConflictValidator {
    public Validation validate(List<TriggerStorySubscription> matches) {
        List<TriggerStorySubscription> checked = List.copyOf(Objects.requireNonNull(matches, "matches"));
        if (checked.isEmpty()) {
            return new Validation(Status.MISSING, Optional.empty(), checked);
        }
        if (checked.size() > 1) {
            return new Validation(Status.CONFLICT, Optional.empty(), checked);
        }
        return new Validation(Status.CLAIMING, Optional.of(checked.get(0)), checked);
    }

    public enum Status {
        MISSING,
        CONFLICT,
        CLAIMING
    }

    public record Validation(
            Status status,
            Optional<TriggerStorySubscription> subscription,
            List<TriggerStorySubscription> matches) {
        public Validation {
            status = Objects.requireNonNull(status, "status");
            subscription = Objects.requireNonNull(subscription, "subscription");
            matches = List.copyOf(Objects.requireNonNull(matches, "matches"));
        }
    }
}
