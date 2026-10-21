package io.github.kxyr0.echonpcinteraction.api.identity;

public record NpcInteractionOwnerId(String value) implements Comparable<NpcInteractionOwnerId> {
    public NpcInteractionOwnerId {
        value = InteractionIdSyntax.requireNamespaced(value, "NpcInteractionOwnerId");
    }

    @Override
    public int compareTo(NpcInteractionOwnerId other) {
        return value.compareTo(other.value);
    }
}
