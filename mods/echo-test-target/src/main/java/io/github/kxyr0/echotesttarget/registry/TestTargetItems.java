package io.github.kxyr0.echotesttarget.registry;

import io.github.kxyr0.echotesttarget.EchoTestTargetMod;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TestTargetItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EchoTestTargetMod.MOD_ID);

    public static final RegistryObject<Item> IMMORTAL_TARGET_SPAWN_EGG = ITEMS.register(
            "immortal_target_spawn_egg",
            () -> new ForgeSpawnEggItem(TestTargetEntities.IMMORTAL_TARGET, 0x31552D, 0x91E56A,
                    new Item.Properties()));

    private TestTargetItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(IMMORTAL_TARGET_SPAWN_EGG);
        }
    }
}

