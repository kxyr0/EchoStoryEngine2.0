package io.github.kxyr0.echoevents.api.event;

import io.github.kxyr0.echoevents.api.subscription.EventSubscription;
import java.util.List;
import java.util.Objects;

/** A source outbox pins eligibility before its authoritative state commit. */
public record EventPublication(EventEnvelope<?> event, List<EventSubscription> subscriptions) {
    public EventPublication {
        Objects.requireNonNull(event, "event");
        subscriptions = List.copyOf(subscriptions);
        if (subscriptions.stream().anyMatch(value -> !value.filter().matches(event))) {
            throw new IllegalArgumentException("Outbox contains an ineligible subscription");
        }
    }
}
