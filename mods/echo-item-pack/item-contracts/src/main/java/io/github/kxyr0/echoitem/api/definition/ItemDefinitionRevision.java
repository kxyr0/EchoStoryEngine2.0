package io.github.kxyr0.echoitem.api.definition;

/**
 * Monotonic immutable definition revision.
 */
public record ItemDefinitionRevision(long value) implements Comparable<ItemDefinitionRevision> {
    public static final ItemDefinitionRevision ZERO = new ItemDefinitionRevision(0L);

    public ItemDefinitionRevision {
        if (value < 0L) {
            throw new IllegalArgumentException("Revision must not be negative.");
        }
    }

    public ItemDefinitionRevision next() {
        return new ItemDefinitionRevision(value + 1L);
    }

    @Override
    public int compareTo(ItemDefinitionRevision other) {
        return Long.compare(value, other.value);
    }
}
