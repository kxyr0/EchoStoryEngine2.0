package io.github.kxyr0.echodebug.forge.registry;

import io.github.kxyr0.echodebug.forge.EchoDebugMod;
import io.github.kxyr0.echodebug.forge.item.TriggerWandItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EchoDebugItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
            EchoDebugMod.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB, EchoDebugMod.MOD_ID);

    public static final RegistryObject<Item> TRIGGER_WAND = ITEMS.register("trigger_wand",
            () -> new TriggerWandItem(new Item.Properties().stacksTo(1)));

    static {
        TABS.register("tools", EchoDebugItems::creativeTab);
    }

    private EchoDebugItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }

    private static CreativeModeTab creativeTab() {
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.echo_debug.tools"))
                .icon(() -> new ItemStack(Items.STICK))
                .displayItems((parameters, output) -> output.accept(TRIGGER_WAND.get()))
                .build();
    }
}
