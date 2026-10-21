package io.github.kxyr0.echoiteminspection.api;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.nbt.CompoundTag;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;

/** An isolated presentation snapshot; callers never receive its mutable backing stack. */
public final class ItemStackSnapshot {
    private final ItemStack stack;
    private final String itemId;
    private final String fingerprint;

    public ItemStackSnapshot(ItemStack source) {
        Objects.requireNonNull(source, "source");
        if (source.isEmpty()) throw new IllegalArgumentException("Cannot inspect an empty stack");
        stack = source.copy();
        itemId = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(stack.getItem())).toString();
        fingerprint = Hashing.sha256().hashString(stack.save(new CompoundTag()).toString(),
                StandardCharsets.UTF_8).toString();
    }

    public ItemStack copyStack() {
        return stack.copy();
    }

    public String itemId() {
        return itemId;
    }

    public int count() {
        return stack.getCount();
    }

    public String fingerprint() { return fingerprint; }
}
