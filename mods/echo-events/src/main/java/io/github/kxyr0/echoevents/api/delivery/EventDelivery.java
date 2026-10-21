package io.github.kxyr0.echoevents.api.delivery;

import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.identity.EventIds.EventOccurrenceId;
import io.github.kxyr0.echoevents.api.identity.EventIds.EventSubscriptionId;
import io.github.kxyr0.echoevents.api.subscription.EventSubscription;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public record EventDelivery(EventEnvelope<?> event, EventSubscription subscription, int attempts,
        long nextTick, Status status, String diagnostic) {
    public EventDelivery {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(subscription, "subscription");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(diagnostic, "diagnostic");
        if (attempts < 0 || nextTick < 0) throw new IllegalArgumentException("Invalid delivery state");
    }
    public Key key() { return new Key(subscription.id(), event.id()); }
    public String executionToken() {
        String key = subscription.id().value() + "\u0000" + event.id().value();
        return "event-reaction-" + UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }
    public enum Status { PENDING, ACCEPTED, FAILED }
    public record Key(EventSubscriptionId subscription, EventOccurrenceId occurrence) {
        public Key {
            Objects.requireNonNull(subscription, "subscription");
            Objects.requireNonNull(occurrence, "occurrence");
        }
    }
}
