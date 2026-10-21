package io.github.kxyr0.echonpcinteraction.api.identity;

public record NpcInteractionCommandId(String value) implements Comparable<NpcInteractionCommandId> {
    public NpcInteractionCommandId {
        value = InteractionIdSyntax.requireNamespaced(value, "NpcInteractionCommandId");
    }

    @Override
    public int compareTo(NpcInteractionCommandId other) {
        return value.compareTo(other.value);
    }
}
