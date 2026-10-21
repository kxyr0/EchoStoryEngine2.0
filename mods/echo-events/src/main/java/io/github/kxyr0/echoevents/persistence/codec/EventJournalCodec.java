package io.github.kxyr0.echoevents.persistence.codec;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.delivery.EventDelivery;
import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.identity.EventIds.EventOccurrenceId;
import io.github.kxyr0.echoevents.api.identity.EventIds.EventSubscriptionId;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId;
import io.github.kxyr0.echoevents.api.subscription.EventFilter;
import io.github.kxyr0.echoevents.api.subscription.EventSubscription;
import io.github.kxyr0.echoevents.core.journal.EventJournalImage;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

public final class EventJournalCodec {
    private final EventSourceCodec sources = new EventSourceCodec();
    private final EventPayloadCodec payloads = new EventPayloadCodec();

    public void write(DataOutput output, EventJournalImage image) throws IOException {
        output.writeInt(image.schema());
        output.writeInt(image.occurrences().size());
        for (var event : image.occurrences().values().stream()
                .sorted(java.util.Comparator.comparing(value -> value.id().value())).toList()) {
            output.writeUTF(event.id().value());
            sources.write(output, event.source());
            payloads.write(output, event.payload());
        }
        output.writeInt(image.deliveries().size());
        for (var delivery : image.deliveries().values().stream().sorted(java.util.Comparator
                .comparing((EventDelivery value) -> value.event().id().value())
                .thenComparing(value -> value.subscription().id())).toList()) writeDelivery(output, delivery);
        output.writeInt(image.onceReceipts().size());
        for (String once : image.onceReceipts().stream().sorted().toList()) output.writeUTF(once);
        output.writeInt(image.causalCounts().size());
        for (String key : image.causalCounts().keySet().stream().sorted().toList()) {
            output.writeUTF(key);
            output.writeInt(image.causalCounts().get(key));
        }
        output.writeInt(image.executionContexts().size());
        var contexts = new EventExecutionContextCodec();
        for (var key : image.executionContexts().keySet().stream()
                .sorted(java.util.Comparator.comparing(value -> value.value())).toList()) {
            output.writeUTF(key.value());
            contexts.write(output, image.executionContexts().get(key));
        }
    }
    public EventJournalImage read(DataInput input) throws IOException {
        int schema = input.readInt();
        if (schema != 1) throw new IOException("Unsupported event schema: " + schema);
        var events = new HashMap<EventOccurrenceId, EventEnvelope<?>>();
        for (int count = count(input); count > 0; count--) {
            var id = new EventOccurrenceId(input.readUTF());
            var event = new EventEnvelope<>(id, sources.read(input), payloads.read(input));
            if (!event.kind().durable() || events.put(id, event) != null) throw new IOException("Invalid occurrence");
        }
        var deliveries = new HashMap<EventDelivery.Key, EventDelivery>();
        for (int count = count(input); count > 0; count--) {
            var event = events.get(new EventOccurrenceId(input.readUTF()));
            if (event == null) throw new IOException("Missing occurrence");
            var delivery = new EventDelivery(event, readSubscription(input), input.readInt(), input.readLong(),
                    EventDelivery.Status.valueOf(input.readUTF()), input.readUTF());
            if (deliveries.put(delivery.key(), delivery) != null) throw new IOException("Duplicate delivery");
        }
        var once = new HashSet<String>();
        for (int count = count(input); count > 0; count--) {
            if (!once.add(input.readUTF())) throw new IOException("Duplicate once receipt");
        }
        var causal = new HashMap<String, Integer>();
        for (int count = count(input); count > 0; count--) {
            String key = input.readUTF();
            int value = input.readInt();
            if (value < 1 || causal.put(key, value) != null) throw new IOException("Invalid causal count");
        }
        var contexts = new HashMap<io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId,
                io.github.kxyr0.echoevents.api.context.EventExecutionContext>();
        var contextCodec = new EventExecutionContextCodec();
        for (int count = count(input); count > 0; count--) {
            var key = new io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId(input.readUTF());
            if (contexts.put(key, contextCodec.read(input)) != null) throw new IOException("Duplicate execution context");
        }
        return new EventJournalImage(schema, events, deliveries, once, causal, contexts);
    }
    private void writeDelivery(DataOutput output, EventDelivery delivery) throws IOException {
        output.writeUTF(delivery.event().id().value());
        var subscription = delivery.subscription();
        output.writeUTF(subscription.id().value());
        output.writeUTF(subscription.filter().kind().stableId());
        output.writeUTF(subscription.filter().resource().value());
        output.writeBoolean(subscription.filter().detail().isPresent());
        if (subscription.filter().detail().isPresent()) output.writeUTF(subscription.filter().detail().orElseThrow().value());
        output.writeBoolean(subscription.filter().subject().isPresent());
        if (subscription.filter().subject().isPresent()) output.writeUTF(subscription.filter().subject().get().value());
        output.writeUTF(subscription.revision());
        output.writeBoolean(subscription.once());
        output.writeInt(delivery.attempts());
        output.writeLong(delivery.nextTick());
        output.writeUTF(delivery.status().name());
        output.writeUTF(delivery.diagnostic());
    }
    private EventSubscription readSubscription(DataInput input) throws IOException {
        var id = new EventSubscriptionId(input.readUTF());
        var kind = EventKind.fromStableId(input.readUTF());
        var resource = new ResourceId(input.readUTF());
        Optional<SymbolId> detail = input.readBoolean() ? Optional.of(new SymbolId(input.readUTF())) : Optional.empty();
        Optional<SymbolId> subject = input.readBoolean() ? Optional.of(new SymbolId(input.readUTF())) : Optional.empty();
        return new EventSubscription(id, new EventFilter(kind, resource, detail, subject), input.readUTF(), input.readBoolean());
    }
    private static int count(DataInput input) throws IOException {
        int count = input.readInt();
        if (count < 0 || count > 1_000_000) throw new IOException("Invalid event collection size");
        return count;
    }
}
