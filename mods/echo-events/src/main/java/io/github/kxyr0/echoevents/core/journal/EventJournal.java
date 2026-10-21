package io.github.kxyr0.echoevents.core.journal;

import io.github.kxyr0.echoevents.api.delivery.EventDelivery;
import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.identity.EventIds.EventOccurrenceId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public final class EventJournal {
    private final EventJournalStore store;
    private EventJournalImage image;

    public EventJournal(EventJournalStore store) {
        this.store = java.util.Objects.requireNonNull(store, "store");
        image = store.load();
    }
    public EventJournalImage image() { return image; }
    public void markOnce(String key) {
        var once = new HashSet<>(image.onceReceipts());
        if (once.add(key)) commit(new EventJournalImage(1, image.occurrences(), image.deliveries(), once,
                image.causalCounts(), image.executionContexts()));
    }
    public Optional<EventEnvelope<?>> find(EventOccurrenceId id) { return Optional.ofNullable(image.occurrences().get(id)); }
    public boolean append(EventEnvelope<?> event, List<EventDelivery> eligible) {
        if (image.occurrences().containsKey(event.id())) return false;
        var occurrences = new HashMap<>(image.occurrences());
        occurrences.put(event.id(), event);
        var deliveries = new HashMap<>(image.deliveries());
        eligible.forEach(delivery -> deliveries.put(delivery.key(), delivery));
        commit(new EventJournalImage(1, occurrences, deliveries, image.onceReceipts(), image.causalCounts(), image.executionContexts()));
        return true;
    }
    public void update(EventDelivery delivery, Optional<String> onceKey, Optional<String> causalKey) {
        var deliveries = new HashMap<>(image.deliveries());
        if (!deliveries.containsKey(delivery.key())) throw new IllegalArgumentException("Unknown delivery");
        deliveries.put(delivery.key(), delivery);
        var once = new HashSet<>(image.onceReceipts());
        onceKey.ifPresent(once::add);
        var causal = new HashMap<>(image.causalCounts());
        causalKey.ifPresent(key -> causal.merge(key, 1, Integer::sum));
        commit(new EventJournalImage(1, image.occurrences(), deliveries, once, causal, image.executionContexts()));
    }
    public void rememberContexts(java.util.Map<io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId,
            io.github.kxyr0.echoevents.api.context.EventExecutionContext> contexts) {
        var combined = new HashMap<>(image.executionContexts());
        contexts.forEach((id, context) -> {
            var existing = combined.putIfAbsent(id, context);
            if (existing != null && !existing.equals(context)) throw new IllegalArgumentException("Conflicting execution context");
        });
        if (!combined.equals(image.executionContexts())) commit(new EventJournalImage(1, image.occurrences(),
                image.deliveries(), image.onceReceipts(), image.causalCounts(), combined));
    }
    private void commit(EventJournalImage next) {
        store.commit(next);
        image = next;
    }
}
