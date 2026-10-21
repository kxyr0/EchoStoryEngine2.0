package io.github.kxyr0.echonpcbackpack.forge.sophisticated;

import io.github.kxyr0.echonpcbackpack.api.payload.NpcBackpackPayloadRef;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;

/**
 * Audited API path for exact stack identity; cloneBackpack is deliberately never used.
 */
public final class SophisticatedBackpackAccess {
    private final BackpackFingerprint fingerprints = new BackpackFingerprint();

    public Optional<BackpackPayloadSnapshot> inspect(ServerLevel level, BlockPos position) {
        if (!(level.getBlockState(position).getBlock() instanceof BackpackBlock)
                || !(level.getBlockEntity(position) instanceof BackpackBlockEntity blockEntity)) {
            return Optional.empty();
        }
        ItemStack stack = blockEntity.getBackpackWrapper().getBackpack().copy();
        Optional<UUID> contentsUuid = blockEntity.getBackpackWrapper().getContentsUuid();
        if (stack.isEmpty() || !(stack.getItem() instanceof BackpackItem) || contentsUuid.isEmpty()) {
            return Optional.empty();
        }
        stack.setCount(1);
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String fingerprint = fingerprints.exact(stack, contentsUuid.orElseThrow());
        return Optional.of(new BackpackPayloadSnapshot(stack,
                new NpcBackpackPayloadRef(contentsUuid.orElseThrow(), itemId, fingerprint),
                BackpackItem.getMainColor(stack), BackpackItem.getAccentColor(stack)));
    }

    public RemovalResult removeExact(ServerLevel level, BlockPos position, String expectedFingerprint) {
        Optional<BackpackPayloadSnapshot> current = inspect(level, position);
        if (current.isEmpty()) return new RemovalResult(Status.MISSING, Optional.empty());
        if (!current.orElseThrow().reference().fingerprint().equals(expectedFingerprint)) {
            return new RemovalResult(Status.CHANGED, current);
        }
        if (!level.removeBlock(position, false)) {
            return new RemovalResult(Status.FAILED, current);
        }
        return new RemovalResult(Status.REMOVED, current);
    }

    public boolean exactAt(ServerLevel level, BlockPos position, ItemStack expected) {
        Optional<BackpackPayloadSnapshot> actual = inspect(level, position);
        if (actual.isEmpty() || expected.isEmpty()) return false;
        Optional<UUID> uuid = contentsUuid(expected);
        return uuid.isPresent() && uuid.orElseThrow().equals(actual.orElseThrow().reference().contentsUuid())
                && fingerprints.exact(expected, uuid.orElseThrow())
                .equals(actual.orElseThrow().reference().fingerprint());
    }

    public Optional<UUID> contentsUuid(ItemStack stack) {
        if (!(stack.getItem() instanceof BackpackItem)) return Optional.empty();
        CompoundTagAccess access = new CompoundTagAccess(stack);
        return access.uuid("contentsUuid");
    }

    public record RemovalResult(Status status, Optional<BackpackPayloadSnapshot> payload) {
    }

    public enum Status { REMOVED, MISSING, CHANGED, FAILED }

    private record CompoundTagAccess(ItemStack stack) {
        Optional<UUID> uuid(String key) {
            return stack.hasTag() && stack.getTag().hasUUID(key)
                    ? Optional.of(stack.getTag().getUUID(key)) : Optional.empty();
        }
    }
}
