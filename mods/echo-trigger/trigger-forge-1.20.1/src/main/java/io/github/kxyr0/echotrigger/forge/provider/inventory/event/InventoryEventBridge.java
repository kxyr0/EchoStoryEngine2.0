package io.github.kxyr0.echotrigger.forge.provider.inventory.event;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.forge.provider.payload.ForgeIds;
import io.github.kxyr0.echotrigger.forge.provider.payload.StackPayloads;
import io.github.kxyr0.echotrigger.forge.service.ForgeTriggerServices;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public final class InventoryEventBridge {
    private final ForgeTriggerServices services;
    private final Map<UUID, InventorySnapshot> snapshots = new LinkedHashMap<>();

    public InventoryEventBridge(ForgeTriggerServices services) {
        this.services = services;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != LogicalSide.SERVER || event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        long tick = player.serverLevel().getGameTime();
        InventorySnapshot current = InventorySnapshot.of(player);
        InventorySnapshot previous = snapshots.put(player.getUUID(), current);
        publishPredicates(player, current, tick);
        if (previous != null) {
            publishDeltas(player, previous, current, tick);
        }
    }

    @SubscribeEvent
    public void onItemPickedUp(PlayerEvent.ItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            publish(player, "ItemPickedUp", StackPayloads.item(event.getStack()));
        }
    }

    @SubscribeEvent
    public void onItemDropped(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            publish(player, "ItemDropped", StackPayloads.item(event.getEntity().getItem()));
        }
    }

    @SubscribeEvent
    public void onItemUsed(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            publish(player, "ItemUsed", StackPayloads.item(event.getItem()));
        }
    }

    @SubscribeEvent
    public void onItemConsumed(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            publish(player, "ItemConsumed", StackPayloads.item(event.getItem()));
        }
    }

    private void publishPredicates(ServerPlayer player, InventorySnapshot snapshot, long tick) {
        TriggerOwnerId owner = ForgeIds.owner(player);
        snapshot.counts().forEach((item, count) -> publishItemPredicates(owner, tick, snapshot.payload(item, count)));
        if (snapshot.emptySlots() > 0) {
            services.applyEvent(TriggerEvent.predicateTrue(owner, tick, "HasEmptyInventorySlot", Map.of()));
        }
    }

    private void publishItemPredicates(TriggerOwnerId owner, long tick, Map<String, String> payload) {
        services.applyEvent(TriggerEvent.predicateTrue(owner, tick, "HasItem", payload));
        services.applyEvent(TriggerEvent.predicateTrue(owner, tick, "ItemCountAtLeast", payload));
        services.applyEvent(TriggerEvent.predicateTrue(owner, tick, "ItemCountAtMost", payload));
        services.applyEvent(TriggerEvent.predicateTrue(owner, tick, "ItemCountExactly", payload));
    }

    private void publishDeltas(
            ServerPlayer player,
            InventorySnapshot previous,
            InventorySnapshot current,
            long tick
    ) {
        current.counts().forEach((item, count) -> publishDelta(player, previous, current,
                item, count - oldCount(previous, item)));
        previous.counts().forEach((item, count) -> publishRemoved(player, previous, current, item, count));
        if (previous.emptySlots() == 0 && current.emptySlots() > 0) {
            services.applyEvent(TriggerEvent.vanilla(ForgeIds.owner(player), tick, "InventorySlotFreed", Map.of()));
        }
        if (previous.emptySlots() > 0 && current.emptySlots() == 0) {
            services.applyEvent(TriggerEvent.vanilla(ForgeIds.owner(player), tick, "InventoryBecameFull", Map.of()));
        }
    }

    private void publishDelta(
            ServerPlayer player,
            InventorySnapshot previous,
            InventorySnapshot current,
            String item,
            int delta
    ) {
        if (delta > 0) {
            publish(player, "ItemAdded", current.payload(item, delta));
        } else if (delta < 0) {
            publish(player, "ItemRemoved", previous.payload(item, -delta));
        }
    }

    private void publishRemoved(
            ServerPlayer player,
            InventorySnapshot previous,
            InventorySnapshot current,
            String item,
            int previousCount
    ) {
        if (!current.counts().containsKey(item)) {
            publish(player, "ItemRemoved", previous.payload(item, previousCount));
        }
    }

    private void publish(Player player, String expressionName, Map<String, String> payload) {
        long tick = player.level().getGameTime();
        services.applyEvent(TriggerEvent.vanilla(ForgeIds.owner(player), tick, expressionName, payload));
    }

    private static int oldCount(InventorySnapshot previous, String item) {
        return previous.counts().getOrDefault(item, 0);
    }
}
