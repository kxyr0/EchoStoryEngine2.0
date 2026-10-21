package io.github.kxyr0.echotrigger.forge.provider.payload;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

public final class StackPayloads {
    private StackPayloads() {
    }

    public static Map<String, String> item(ItemStack stack) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("item", ForgeIds.item(stack));
        payload.put("target", ForgeIds.item(stack));
        payload.put("count", Integer.toString(stack.getCount()));
        payload.put("tags", ForgeIds.itemTags(stack));
        payload.put("itemTags", ForgeIds.itemTags(stack));
        return payload;
    }

    public static Map<String, String> changed(ItemStack previous, ItemStack current) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("from", ForgeIds.item(previous));
        payload.put("to", ForgeIds.item(current));
        payload.put("previousItem", ForgeIds.item(previous));
        payload.put("currentItem", ForgeIds.item(current));
        payload.put("previousCount", Integer.toString(previous.getCount()));
        payload.put("currentCount", Integer.toString(current.getCount()));
        payload.put("fromTags", ForgeIds.itemTags(previous));
        payload.put("toTags", ForgeIds.itemTags(current));
        return payload;
    }
}
