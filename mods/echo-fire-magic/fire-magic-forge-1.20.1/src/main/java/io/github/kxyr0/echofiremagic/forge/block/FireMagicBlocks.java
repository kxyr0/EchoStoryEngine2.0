package io.github.kxyr0.echofiremagic.forge.block;

import io.github.kxyr0.echofiremagic.forge.EchoFireMagicMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class FireMagicBlocks {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, EchoFireMagicMod.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EchoFireMagicMod.MOD_ID);

    public static final RegistryObject<Block> BLACKSMITH_BLANK = BLOCKS.register("blacksmith_blank",
            () -> new BlacksmithBlankBlock(BlockBehaviour.Properties.of().strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(BlacksmithBlankBlock.HEATED) ? 10 : 0)));
    public static final RegistryObject<Item> BLACKSMITH_BLANK_ITEM = ITEMS.register("blacksmith_blank",
            () -> new BlockItem(BLACKSMITH_BLANK.get(), new Item.Properties()));

    private FireMagicBlocks() { }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(BLACKSMITH_BLANK_ITEM);
        }
    }
}
