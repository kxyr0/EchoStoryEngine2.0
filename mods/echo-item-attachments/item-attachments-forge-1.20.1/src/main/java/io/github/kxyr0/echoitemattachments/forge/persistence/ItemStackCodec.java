package io.github.kxyr0.echoitemattachments.forge.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;

public final class ItemStackCodec {
    public byte[] encode(ItemStack stack) {
        if (stack.isEmpty()) throw new IllegalArgumentException("cannot encode an empty stack");
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(stack.save(new CompoundTag()), output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not encode ItemStack", exception);
        }
    }

    public ItemStack decode(byte[] bytes) {
        try {
            CompoundTag tag = NbtIo.readCompressed(new ByteArrayInputStream(bytes));
            ItemStack stack = ItemStack.of(tag);
            if (stack.isEmpty()) throw new IllegalStateException("Journal contains an empty stack");
            return stack;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not decode ItemStack", exception);
        }
    }
}
