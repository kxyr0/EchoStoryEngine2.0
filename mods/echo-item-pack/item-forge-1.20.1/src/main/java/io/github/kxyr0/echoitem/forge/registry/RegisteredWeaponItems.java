package io.github.kxyr0.echoitem.forge.registry;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.forge.EchoItemPackMod;
import io.github.kxyr0.echoitem.forge.ForgeItemRuntimeState;
import io.github.kxyr0.echoitem.forge.item.EchoWeaponItem;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Registers generic runtime weapon item objects discovered before registry freeze.
 */
public final class RegisteredWeaponItems {
    private final ForgeItemRuntimeState state;
    private final Map<EchoItemId, EchoWeaponItem> items = new LinkedHashMap<>();

    public RegisteredWeaponItems(ForgeItemRuntimeState state) {
        this.state = state;
    }

    public void onRegister(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ITEMS, registry -> {
            for (EchoItemId id : state.registeredIds()) {
                EchoWeaponItem item = new EchoWeaponItem(id, state.catalog());
                items.put(id, item);
                registry.register(resource(id), item);
            }
        });
        event.register(Registries.CREATIVE_MODE_TAB, registry -> registry.register(
                new ResourceLocation(EchoItemPackMod.MOD_ID, "echo_items"), creativeTab()));
    }

    public Item item(EchoItemId id) {
        return items.get(id);
    }

    public List<ItemStack> creativeStacks() {
        return items.entrySet().stream()
                .filter(entry -> !state.catalog().current(entry.getKey()).hiddenFromCreative())
                .sorted(Comparator.comparing(entry -> orderKey(entry.getKey())))
                .map(entry -> new ItemStack(entry.getValue()))
                .toList();
    }

    private CreativeModeTab creativeTab() {
        return CreativeModeTab.builder()
                .title(Component.literal("Echo Items"))
                .withSearchBar()
                .icon(() -> new ItemStack(Items.IRON_SWORD))
                .displayItems((parameters, output) -> creativeStacks().forEach(output::accept))
                .build();
    }

    private String orderKey(EchoItemId id) {
        var definition = state.catalog().current(id);
        return definition.creativeOrder().isPresent()
                ? String.format("%08d:%s", definition.creativeOrder().orElseThrow(), id.value())
                : "zzzzzzzz:" + id.value();
    }

    private ResourceLocation resource(EchoItemId id) {
        return new ResourceLocation(id.namespace(), id.path());
    }
}
