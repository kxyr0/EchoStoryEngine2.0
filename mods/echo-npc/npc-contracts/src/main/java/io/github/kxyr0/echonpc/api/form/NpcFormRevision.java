package io.github.kxyr0.echonpc.api.form;

/**
 * Monotonic catalog revision carried by one compiled form snapshot.
 */
public record NpcFormRevision(long value) implements Comparable<NpcFormRevision> {
    public NpcFormRevision {
        if (value < 0L) {
            throw new IllegalArgumentException("NpcFormRevision must not be negative.");
        }
    }

    @Override
    public int compareTo(NpcFormRevision other) {
        return Long.compare(value, other.value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
