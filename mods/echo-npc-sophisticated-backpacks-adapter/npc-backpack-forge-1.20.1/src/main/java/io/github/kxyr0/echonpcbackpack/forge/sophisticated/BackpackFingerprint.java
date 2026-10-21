package io.github.kxyr0.echonpcbackpack.forge.sophisticated;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;

public final class BackpackFingerprint {
    public String exact(ItemStack stack, UUID contentsUuid) {
        CompoundTag stackTag = stack.save(new CompoundTag());
        CompoundTag contents = BackpackStorage.get().getOrCreateBackpackContents(contentsUuid).copy();
        return sha256(stackTag + "\n" + contentsUuid + "\n" + contents);
    }

    public String stack(ItemStack stack) {
        return sha256(stack.save(new CompoundTag()).toString());
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
