package io.github.kxyr0.echonpcinteraction.api.identity;

public record NpcInteractionExecutionId(String value) implements Comparable<NpcInteractionExecutionId> {
    public NpcInteractionExecutionId {
        value = InteractionIdSyntax.requireNamespaced(value, "NpcInteractionExecutionId");
    }

    @Override
    public int compareTo(NpcInteractionExecutionId other) {
        return value.compareTo(other.value);
    }
}
