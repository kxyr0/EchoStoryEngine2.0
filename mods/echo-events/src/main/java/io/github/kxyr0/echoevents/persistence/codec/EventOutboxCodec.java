package io.github.kxyr0.echoevents.persistence.codec;

import io.github.kxyr0.echoevents.api.delivery.EventDelivery;
import io.github.kxyr0.echoevents.api.event.EventPublication;
import io.github.kxyr0.echoevents.core.journal.EventJournalImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Uses the journal's versioned codec; source checkpoints contain no Java class names. */
public final class EventOutboxCodec {
    public String encode(List<EventPublication> publications) {
        var events = new HashMap<io.github.kxyr0.echoevents.api.identity.EventIds.EventOccurrenceId,
                io.github.kxyr0.echoevents.api.event.EventEnvelope<?>>();
        var deliveries = new HashMap<EventDelivery.Key, EventDelivery>();
        for (var publication : publications) {
            events.put(publication.event().id(), publication.event());
            for (var subscription : publication.subscriptions()) {
                var delivery = new EventDelivery(publication.event(), subscription, 0, 0,
                        EventDelivery.Status.PENDING, "");
                deliveries.put(delivery.key(), delivery);
            }
        }
        try {
            var bytes = new ByteArrayOutputStream();
            new EventJournalCodec().write(new DataOutputStream(bytes),
                    new EventJournalImage(1, events, deliveries, Set.of(), Map.of()));
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException failure) { throw new IllegalStateException("Cannot encode event outbox", failure); }
    }

    public List<EventPublication> decode(String encoded) {
        try {
            var input = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)));
            var image = new EventJournalCodec().read(input);
            if (input.available() != 0) throw new IOException("Trailing event outbox bytes");
            return image.occurrences().values().stream().sorted(java.util.Comparator.comparing(value -> value.id().value()))
                    .map(event -> new EventPublication(event, image.deliveries().values().stream()
                            .filter(value -> value.event().id().equals(event.id())).map(EventDelivery::subscription)
                            .sorted(java.util.Comparator.comparing(value -> value.id())).toList())).toList();
        } catch (IOException | IllegalArgumentException failure) {
            throw new IllegalStateException("Invalid event outbox; refusing to recompute eligibility", failure);
        }
    }
}
