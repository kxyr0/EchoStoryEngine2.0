package io.github.kxyr0.echonpcinteraction.forge.consequence;

import io.github.kxyr0.echonpcinteraction.forge.inspect.BlockStateFingerprint;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

public final class InteractionEffectJournal {
    private static final double ENTITY_RADIUS = 3.0D;

    public InteractionEffectSnapshot capture(ServerLevel level, BlockPos target, ServerPlayer proxy) {
        Entity vehicle = proxy.getVehicle();
        return new InteractionEffectSnapshot(BlockStateFingerprint.state(level.getBlockState(target)),
                blockEntity(level.getBlockEntity(target)), BlockStateFingerprint.neighborhood(level, target),
                entities(level, target, proxy), Optional.ofNullable(vehicle).map(Entity::getUUID),
                Optional.ofNullable(vehicle).map(value -> entityKey(value.getType())),
                proxy.containerMenu != proxy.inventoryMenu, inventory(proxy), proxy.position(),
                proxy.getPose().name());
    }

    private Map<UUID, String> entities(ServerLevel level, BlockPos target, ServerPlayer proxy) {
        Map<UUID, String> observed = new LinkedHashMap<>();
        AABB area = new AABB(target).inflate(ENTITY_RADIUS);
        level.getEntities(proxy, area, entity -> !entity.isRemoved())
                .forEach(entity -> observed.put(entity.getUUID(), entityKey(entity.getType())));
        return observed;
    }

    private String blockEntity(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return "none";
        }
        try {
            return blockEntityKey(blockEntity.getType()) + "|" + blockEntity.getUpdateTag().hashCode();
        } catch (RuntimeException exception) {
            return blockEntityKey(blockEntity.getType()) + "|unavailable";
        }
    }

    private String inventory(ServerPlayer proxy) {
        StringJoiner joiner = new StringJoiner(";");
        for (int index = 0; index < proxy.getInventory().getContainerSize(); index++) {
            ItemStack stack = proxy.getInventory().getItem(index);
            if (!stack.isEmpty()) {
                joiner.add(index + "=" + itemKey(stack.getItem()) + "x" + stack.getCount());
            }
        }
        return joiner.toString();
    }

    private String itemKey(net.minecraft.world.item.Item item) {
        return String.valueOf(ForgeRegistries.ITEMS.getKey(item));
    }

    private String entityKey(EntityType<?> type) {
        return String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(type));
    }

    private String blockEntityKey(BlockEntityType<?> type) {
        return String.valueOf(ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(type));
    }
}
