package io.github.kxyr0.echobloodmagic.forge.item;

import io.github.kxyr0.echobloodmagic.forge.EchoBloodMagicMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BloodMagicItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EchoBloodMagicMod.MOD_ID);
    public static final RegistryObject<Item> BLOOD_RAPIER = ITEMS.register("blood_rapier", BloodRapierItem::new);

    private BloodMagicItems() { }
    public static void register(IEventBus bus) { ITEMS.register(bus); }
}
