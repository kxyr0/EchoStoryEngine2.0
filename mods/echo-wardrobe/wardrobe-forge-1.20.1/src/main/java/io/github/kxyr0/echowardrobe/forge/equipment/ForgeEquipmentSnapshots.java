package io.github.kxyr0.echowardrobe.forge.equipment;

import io.github.kxyr0.echowardrobe.api.slot.WardrobeEquipmentSlot;
import io.github.kxyr0.echowardrobe.core.equipment.EquipmentItemSnapshot;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Converts current vanilla equipment into plain item IDs.
 */
public final class ForgeEquipmentSnapshots {
    public static EquipmentItemSnapshot currentStatic(Player player) {
        return new ForgeEquipmentSnapshots().current(player);
    }

    public EquipmentItemSnapshot current(Player player) {
        Map<WardrobeEquipmentSlot, String> ids = new EnumMap<>(WardrobeEquipmentSlot.class);
        put(ids, WardrobeEquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.HEAD));
        put(ids, WardrobeEquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST));
        put(ids, WardrobeEquipmentSlot.LEGS, player.getItemBySlot(EquipmentSlot.LEGS));
        put(ids, WardrobeEquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.FEET));
        return new EquipmentItemSnapshot(ids);
    }

    private void put(Map<WardrobeEquipmentSlot, String> ids, WardrobeEquipmentSlot slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key != null) {
            ids.put(slot, key.toString());
        }
    }
}
