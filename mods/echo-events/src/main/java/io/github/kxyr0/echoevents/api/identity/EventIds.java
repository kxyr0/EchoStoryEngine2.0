package io.github.kxyr0.echoevents.api.identity;

import java.util.Objects;

public final class EventIds {
    private EventIds() { }

    public static String text(String value) {
        String checked = Objects.requireNonNull(value, "identity").trim();
        if (checked.isEmpty() || checked.length() > 4096) throw new IllegalArgumentException("Invalid identity");
        return checked;
    }

    public record EventOccurrenceId(String value) {
        public EventOccurrenceId { value = text(value); }
    }
    public record EventSubscriptionId(String value) implements Comparable<EventSubscriptionId> {
        public EventSubscriptionId { value = text(value); }
        public int compareTo(EventSubscriptionId other) { return value.compareTo(other.value); }
    }
    public record ResourceId(String value) {
        public ResourceId {
            value = text(value);
            if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+(?:#[a-zA-Z0-9_./-]+)?")) {
                throw new IllegalArgumentException("Resource identity must be namespace:path[#point]");
            }
        }
    }
    public record ExecutionId(String value) {
        public ExecutionId { value = text(value); }
    }
    public record TransitionId(String value) {
        public TransitionId { value = text(value); }
    }
    public record SymbolId(String value) {
        public SymbolId { value = text(value); }
    }
    public record EventSubscriptionIndexRevision(long value) {
        public EventSubscriptionIndexRevision {
            if (value < 0) throw new IllegalArgumentException("Negative index revision");
        }
    }
}
