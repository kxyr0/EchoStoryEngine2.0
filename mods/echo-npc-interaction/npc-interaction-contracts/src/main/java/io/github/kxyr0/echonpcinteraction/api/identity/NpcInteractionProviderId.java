package io.github.kxyr0.echonpcinteraction.api.identity;

public record NpcInteractionProviderId(String value) implements Comparable<NpcInteractionProviderId> {
    public NpcInteractionProviderId {
        value = InteractionIdSyntax.requireNamespaced(value, "NpcInteractionProviderId");
    }

    @Override
    public int compareTo(NpcInteractionProviderId other) {
        return value.compareTo(other.value);
    }
}
