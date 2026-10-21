package io.github.kxyr0.echoevents.api.event;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.context.EventSource;
import io.github.kxyr0.echoevents.api.identity.EventIds.EventOccurrenceId;
import io.github.kxyr0.echoevents.api.payload.EventPayload;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public record EventEnvelope<P extends EventPayload>(EventOccurrenceId id, EventSource source, P payload) {
    public EventEnvelope {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(payload, "payload");
    }
    public EventKind kind() { return payload.kind(); }
    public EventDurability durability() {
        return kind().durable() ? EventDurability.DURABLE : EventDurability.TRANSIENT;
    }
    public static <P extends EventPayload> EventEnvelope<P> occurrence(EventSource source, P payload) {
        return new EventEnvelope<>(identity(source.module(), source.execution(), source.transition(), payload.kind()), source, payload);
    }
    public static EventOccurrenceId identity(String module,
            io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId execution,
            io.github.kxyr0.echoevents.api.identity.EventIds.TransitionId transition, EventKind kind) {
        String identity = module + "\u0000" + execution.value() + "\u0000" + transition.value() + "\u0000" + kind.stableId();
        return new EventOccurrenceId("evt-" + UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)));
    }
    public boolean sameTransition(EventEnvelope<?> other) {
        return id.equals(other.id) && payload.equals(other.payload)
                && source.resource().equals(other.source.resource()) && source.execution().equals(other.source.execution())
                && source.rootExecution().equals(other.source.rootExecution()) && source.audience().equals(other.source.audience())
                && source.definitionRevision().equals(other.source.definitionRevision());
    }
}
