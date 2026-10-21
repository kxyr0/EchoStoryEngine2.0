package io.github.kxyr0.echotrigger.forge.provider.inventory.event;

import io.github.kxyr0.echotrigger.forge.provider.payload.ForgeIds;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

record InventorySnapshot(Map<String, Integer> counts, Map<String, String> tags, int emptySlots) {
    InventorySnapshot {
        counts = Map.copyOf(counts);
        tags = Map.copyOf(tags);
    }

    static InventorySnapshot of(ServerPlayer player) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> tags = new LinkedHashMap<>();
        int emptySlots = 0;
        for (ItemStack stack : player.getInventory().items) {
            emptySlots += stack.isEmpty() ? 1 : 0;
            add(stack, counts, tags);
        }
        add(player.getOffhandItem(), counts, tags);
        return new InventorySnapshot(counts, tags, emptySlots);
    }

    Map<String, String> payload(String item, int count) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("item", item);
        payload.put("target", item);
        payload.put("count", Integer.toString(count));
        payload.put("tags", tags.getOrDefault(item, ""));
        payload.put("itemTags", tags.getOrDefault(item, ""));
        return payload;
    }

    private static void add(ItemStack stack, Map<String, Integer> counts, Map<String, String> tags) {
        if (stack.isEmpty()) {
            return;
        }
        String item = ForgeIds.item(stack);
        counts.merge(item, stack.getCount(), Integer::sum);
        tags.putIfAbsent(item, ForgeIds.itemTags(stack));
    }
}
