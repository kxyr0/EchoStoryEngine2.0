package io.github.kxyr0.echotrigger.forge.provider.equipment.event;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.forge.provider.payload.ForgeIds;
import io.github.kxyr0.echotrigger.forge.provider.payload.StackPayloads;
import io.github.kxyr0.echotrigger.forge.service.ForgeTriggerServices;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public final class EquipmentEventBridge {
    private final ForgeTriggerServices services;

    public EquipmentEventBridge(ForgeTriggerServices services) {
        this.services = services;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END
                && event.player instanceof ServerPlayer player) {
            publishHandPredicates(player);
            publishArmorPredicates(player);
        }
    }

    @SubscribeEvent
    public void onEquipmentChanged(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || sameIdentity(event.getFrom(), event.getTo())) {
            return;
        }
        if (event.getSlot() == EquipmentSlot.MAINHAND || event.getSlot() == EquipmentSlot.OFFHAND) {
            publishHandChange(player, event.getSlot(), event.getFrom(), event.getTo());
        } else if (isArmor(event.getSlot())) {
            publishArmorChange(player, event.getSlot(), event.getFrom(), event.getTo());
        }
    }

    private void publishHandPredicates(ServerPlayer player) {
        publishHeld(player, "HoldingMainHand", player.getMainHandItem());
        publishHeld(player, "HoldingOffHand", player.getOffhandItem());
        publishHeld(player, "Holding", player.getMainHandItem());
        publishHeld(player, "Holding", player.getOffhandItem());
    }

    private void publishArmorPredicates(ServerPlayer player) {
        for (EquipmentSlot slot : armorSlots()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                services.applyEvent(TriggerEvent.predicateTrue(ForgeIds.owner(player), tick(player),
                        "Wearing", slotPayload(slot, stack)));
            }
        }
        publishAnyArmor(player);
        publishFullArmorTags(player);
    }

    private void publishHandChange(
            ServerPlayer player,
            EquipmentSlot slot,
            ItemStack previous,
            ItemStack current
    ) {
        Map<String, String> payload = StackPayloads.changed(previous, current);
        payload.put("hand", handSlot(slot));
        publish(player, "HeldItemChanged", payload);
        publish(player, slot == EquipmentSlot.OFFHAND ? "OffHandChanged" : "MainHandChanged", payload);
    }

    private void publishArmorChange(
            ServerPlayer player,
            EquipmentSlot slot,
            ItemStack previous,
            ItemStack current
    ) {
        Map<String, String> currentPayload = slotPayload(slot, current);
        publish(player, "EquipmentChanged", currentPayload);
        if (!current.isEmpty()) {
            publish(player, "EquipmentEquipped", currentPayload);
        }
        if (!previous.isEmpty()) {
            publish(player, "EquipmentUnequipped", slotPayload(slot, previous));
        }
    }

    private void publishHeld(ServerPlayer player, String expressionName, ItemStack stack) {
        if (!stack.isEmpty()) {
            services.applyEvent(TriggerEvent.predicateTrue(ForgeIds.owner(player), tick(player),
                    expressionName, StackPayloads.item(stack)));
        }
    }

    private void publishAnyArmor(ServerPlayer player) {
        for (EquipmentSlot slot : armorSlots()) {
            if (!player.getItemBySlot(slot).isEmpty()) {
                services.applyEvent(TriggerEvent.predicateTrue(ForgeIds.owner(player), tick(player),
                        "HasAnyArmor", Map.of()));
                return;
            }
        }
    }

    private void publishFullArmorTags(ServerPlayer player) {
        Set<String> tags = commonArmorTags(player);
        if (!tags.isEmpty()) {
            services.applyEvent(TriggerEvent.predicateTrue(ForgeIds.owner(player), tick(player),
                    "HasFullArmorSet", Map.of("tags", String.join(",", tags))));
        }
    }

    private Set<String> commonArmorTags(ServerPlayer player) {
        Set<String> common = null;
        for (EquipmentSlot slot : armorSlots()) {
            Set<String> tags = itemTagSet(player.getItemBySlot(slot));
            if (tags.isEmpty()) {
                return Set.of();
            }
            common = common == null ? tags : retain(common, tags);
        }
        return common == null ? Set.of() : common;
    }

    private static Map<String, String> slotPayload(EquipmentSlot slot, ItemStack stack) {
        Map<String, String> payload = StackPayloads.item(stack);
        payload.put("slot", slot.name());
        return payload;
    }

    private static Set<String> itemTagSet(ItemStack stack) {
        Set<String> tags = new LinkedHashSet<>();
        stack.getTags().map(TagKey<Item>::location).forEach(tag -> tags.add(tag.toString()));
        return tags;
    }

    private static Set<String> retain(Set<String> first, Set<String> second) {
        first.retainAll(second);
        return first;
    }

    private static EquipmentSlot[] armorSlots() {
        return new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    }

    private void publish(ServerPlayer player, String expressionName, Map<String, String> payload) {
        services.applyEvent(TriggerEvent.vanilla(ForgeIds.owner(player), tick(player), expressionName, payload));
    }

    private static boolean sameIdentity(ItemStack previous, ItemStack current) {
        return ForgeIds.item(previous).equals(ForgeIds.item(current));
    }

    private static String handSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.OFFHAND ? "OFF_HAND" : "MAIN_HAND";
    }

    private static boolean isArmor(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }

    private static long tick(ServerPlayer player) {
        return player.serverLevel().getGameTime();
    }
}
