package io.github.kxyr0.echonpc.api.identity;

/**
 * Stable caller-supplied identity for one durable actor mutation.
 */
public record ActorMutationId(String value) implements Comparable<ActorMutationId> {
    private static final int MAX_LENGTH = 64;

    public ActorMutationId {
        value = NamespacedIdSyntax.require(value, "ActorMutationId");
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("ActorMutationId must be at most 64 characters.");
        }
    }

    @Override
    public int compareTo(ActorMutationId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
