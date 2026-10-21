package io.github.kxyr0.echowardrobe.forge.content;

import io.github.kxyr0.echowardrobe.core.equipment.ItemRegistryView;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Forge item registry boundary for wearable item binding.
 */
public final class ForgeItemRegistry implements ItemRegistryView {
    @Override
    public boolean itemExists(String itemId) {
        try {
            return ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemId));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
