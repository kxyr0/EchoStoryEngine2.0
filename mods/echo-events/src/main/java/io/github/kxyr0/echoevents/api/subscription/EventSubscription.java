package io.github.kxyr0.echoevents.api.subscription;

import io.github.kxyr0.echoevents.api.identity.EventIds;
import io.github.kxyr0.echoevents.api.identity.EventIds.EventSubscriptionId;
import java.util.Objects;

public record EventSubscription(EventSubscriptionId id, EventFilter filter, String revision, boolean once) {
    public EventSubscription {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(filter, "filter");
        revision = EventIds.text(revision);
    }
}
