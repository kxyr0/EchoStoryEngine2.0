package io.github.kxyr0.echoevents.core.index;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.identity.EventIds.EventSubscriptionId;
import io.github.kxyr0.echoevents.api.identity.EventIds.EventSubscriptionIndexRevision;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.subscription.EventSubscription;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EventSubscriptionIndex {
    private final EventSubscriptionIndexRevision revision;
    private final Map<Key, List<EventSubscription>> buckets;

    public EventSubscriptionIndex(EventSubscriptionIndexRevision revision, List<EventSubscription> subscriptions) {
        this.revision = java.util.Objects.requireNonNull(revision, "revision");
        Map<Key, List<EventSubscription>> building = new LinkedHashMap<>();
        Set<EventSubscriptionId> ids = new HashSet<>();
        for (EventSubscription subscription : subscriptions) {
            if (!ids.add(subscription.id())) throw new IllegalArgumentException("Duplicate subscription: " + subscription.id());
            var key = new Key(subscription.filter().kind(), subscription.filter().resource());
            building.computeIfAbsent(key, ignored -> new ArrayList<>()).add(subscription);
        }
        Map<Key, List<EventSubscription>> frozen = new LinkedHashMap<>();
        building.forEach((key, values) -> frozen.put(key, values.stream()
                .sorted(java.util.Comparator.comparing(EventSubscription::id)).toList()));
        buckets = Map.copyOf(frozen);
    }
    public EventSubscriptionIndexRevision revision() { return revision; }
    public List<EventSubscription> candidates(EventEnvelope<?> event) {
        return buckets.getOrDefault(new Key(event.kind(), event.source().resource()), List.of());
    }
    public List<EventSubscription> subscriptions() {
        return buckets.values().stream().flatMap(List::stream)
                .sorted(java.util.Comparator.comparing(EventSubscription::id)).toList();
    }
    private record Key(EventKind kind, ResourceId resource) { }
}
