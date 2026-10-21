package io.github.kxyr0.echowardrobe.testmod;

import io.github.kxyr0.echowardrobe.testmod.command.EchoWardrobeTestCommands;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Development-only item IDs for wardrobe vertical tests.
 */
@Mod(EchoWardrobeTestmod.MOD_ID)
public final class EchoWardrobeTestmod {
    public static final String MOD_ID = "echo_wardrobe_test";
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<Item> ORIGIN_BOOTS = armor("origin_john_boots", ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> ORIGIN_GLASSES = armor("origin_john_glasses", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> ORIGIN_PANTS = armor("origin_john_pants", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> ORIGIN_ROBE = armor("origin_john_robe", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> CLASSIC_BOOTS = armor("classic_john_boots", ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> CLASSIC_GLASSES = armor("classic_john_glasses", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> CLASSIC_HOODIE = armor("classic_john_hoodie", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> CLASSIC_PANTS = armor("classic_john_pants", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> CLASSIC_ALT_PANTS = armor("classic_john_pants_alternative",
            ArmorItem.Type.LEGGINGS);

    public EchoWardrobeTestmod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        new EchoWardrobeTestCommands().register(event.getDispatcher());
    }

    private static RegistryObject<Item> armor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ArmorItem(ArmorMaterials.LEATHER, type, new Item.Properties()));
    }
}
