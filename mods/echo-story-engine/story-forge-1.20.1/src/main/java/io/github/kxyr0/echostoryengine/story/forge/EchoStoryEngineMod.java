package io.github.kxyr0.echostoryengine.story.forge;

import io.github.kxyr0.echostoryengine.story.forge.composition.ForgeStoryCompositionRoot;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Тонкая Forge-точка входа: только связывает lifecycle events с composition root.
 */
@Mod(EchoStoryEngineMod.MOD_ID)
public final class EchoStoryEngineMod {
    public static final String MOD_ID = "echo_story_engine";

    private final ForgeStoryCompositionRoot compositionRoot;

    public EchoStoryEngineMod(FMLJavaModLoadingContext context) {
        this.compositionRoot = ForgeStoryCompositionRoot.create();
        this.compositionRoot.register(context.getModEventBus(), MinecraftForge.EVENT_BUS);
    }
}
