package io.github.kxyr0.echonpcbackpack.api.identity;

public record NpcBackpackAnimationBindingId(String value) {
    public NpcBackpackAnimationBindingId {
        value = BackpackIdSyntax.namespaced(value, "NpcBackpackAnimationBindingId");
    }
}
