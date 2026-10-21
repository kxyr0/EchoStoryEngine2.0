package io.github.kxyr0.echobackgroundnpc.core.schedule;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Bounded idempotency ledger for one-shot Echo Time occurrence identities. */
public final class OccurrenceLedger {
    private final int capacity;
    private final Set<String> seen = new LinkedHashSet<>();

    public OccurrenceLedger(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive.");
        this.capacity = capacity;
    }

    public synchronized boolean first(String occurrenceId) {
        String id = Objects.requireNonNull(occurrenceId, "occurrenceId");
        if (!seen.add(id)) return false;
        while (seen.size() > capacity) seen.remove(seen.iterator().next());
        return true;
    }

    public synchronized Set<String> snapshot() { return Set.copyOf(seen); }
    public synchronized void restore(Set<String> ids) {
        seen.clear();
        ids.stream().sorted().skip(Math.max(0L, ids.size() - capacity)).forEach(seen::add);
    }
}
