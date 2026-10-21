package io.github.kxyr0.echonpcbackpack.forge.sophisticated;

import io.github.kxyr0.echonpcbackpack.api.payload.NpcBackpackPayloadRef;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

public record BackpackPayloadSnapshot(ItemStack stack, NpcBackpackPayloadRef reference,
        int mainColor, int accentColor) {
    public BackpackPayloadSnapshot {
        stack = Objects.requireNonNull(stack, "stack").copy();
        reference = Objects.requireNonNull(reference, "reference");
        if (stack.isEmpty() || stack.getCount() != 1) {
            throw new IllegalArgumentException("Backpack payload must contain exactly one item.");
        }
    }

    @Override
    public ItemStack stack() {
        return stack.copy();
    }
}
