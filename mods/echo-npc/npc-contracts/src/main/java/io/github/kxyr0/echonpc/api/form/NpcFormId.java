package io.github.kxyr0.echonpc.api.form;

import io.github.kxyr0.echonpc.api.identity.NamespacedIdSyntax;

/**
 * Stable authored body and appearance form identity for one logical NPC.
 */
public record NpcFormId(String value) implements Comparable<NpcFormId> {
    public NpcFormId {
        value = NamespacedIdSyntax.require(value, "NpcFormId");
    }

    @Override
    public int compareTo(NpcFormId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
