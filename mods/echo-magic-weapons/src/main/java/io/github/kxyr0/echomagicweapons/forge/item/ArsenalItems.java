package io.github.kxyr0.echomagicweapons.forge.item;

import io.github.kxyr0.echomagicweapons.forge.EchoMagicWeaponsMod;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ArsenalItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EchoMagicWeaponsMod.MOD_ID);
    private static final Map<ArsenalWeaponKind, RegistryObject<Item>> WEAPONS =
            new EnumMap<>(ArsenalWeaponKind.class);

    static {
        for (ArsenalWeaponKind kind : ArsenalWeaponKind.values()) {
            WEAPONS.put(kind, ITEMS.register(kind.id(), () -> new ArsenalWeaponItem(kind)));
        }
    }

    private ArsenalItems() { }

    public static void register(IEventBus bus) { ITEMS.register(bus); }
    public static Item item(ArsenalWeaponKind kind) { return WEAPONS.get(kind).get(); }

    public static ArsenalWeaponKind kind(ItemStack stack) {
        for (var entry : WEAPONS.entrySet()) if (stack.is(entry.getValue().get())) return entry.getKey();
        return null;
    }
}
