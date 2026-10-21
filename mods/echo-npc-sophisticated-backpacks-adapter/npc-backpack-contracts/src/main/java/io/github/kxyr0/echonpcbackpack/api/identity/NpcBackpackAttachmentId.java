package io.github.kxyr0.echonpcbackpack.api.identity;

public record NpcBackpackAttachmentId(String value) implements Comparable<NpcBackpackAttachmentId> {
    public NpcBackpackAttachmentId {
        value = BackpackIdSyntax.namespaced(value, "NpcBackpackAttachmentId");
    }

    @Override
    public int compareTo(NpcBackpackAttachmentId other) {
        return value.compareTo(other.value);
    }
}
