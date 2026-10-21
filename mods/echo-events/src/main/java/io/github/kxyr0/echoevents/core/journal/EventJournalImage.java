package io.github.kxyr0.echoevents.core.journal;

import io.github.kxyr0.echoevents.api.delivery.EventDelivery;
import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.identity.EventIds.EventOccurrenceId;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record EventJournalImage(int schema, Map<EventOccurrenceId, EventEnvelope<?>> occurrences,
        Map<EventDelivery.Key, EventDelivery> deliveries, Set<String> onceReceipts, Map<String, Integer> causalCounts,
        Map<io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId,
                io.github.kxyr0.echoevents.api.context.EventExecutionContext> executionContexts) {
    public EventJournalImage(int schema, Map<EventOccurrenceId, EventEnvelope<?>> occurrences,
            Map<EventDelivery.Key, EventDelivery> deliveries, Set<String> onceReceipts, Map<String, Integer> causalCounts) {
        this(schema, occurrences, deliveries, onceReceipts, causalCounts, Map.of());
    }
    public EventJournalImage {
        if (schema != 1) throw new IllegalArgumentException("Unsupported event schema: " + schema);
        occurrences = Map.copyOf(occurrences);
        deliveries = Map.copyOf(deliveries);
        onceReceipts = Set.copyOf(onceReceipts);
        causalCounts = Map.copyOf(causalCounts);
        executionContexts = Map.copyOf(executionContexts);
        for (var entry : deliveries.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            if (!key.equals(value.key()) || !value.event().equals(occurrences.get(key.occurrence()))) {
                throw new IllegalArgumentException("Inconsistent event journal");
            }
        }
    }
    public static EventJournalImage empty() {
        return new EventJournalImage(1, Map.of(), Map.of(), Set.of(), Map.of());
    }
    public List<EventDelivery> pending() {
        return deliveries.values().stream().filter(value -> value.status() == EventDelivery.Status.PENDING)
                .sorted(java.util.Comparator.comparingLong(EventDelivery::nextTick)
                        .thenComparing(value -> value.event().source().gameTick())
                        .thenComparing(value -> value.event().id().value())
                        .thenComparing(value -> value.subscription().id())).toList();
    }
}
