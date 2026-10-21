package io.github.kxyr0.echodialogue.forge;

import io.github.kxyr0.echodialogue.forge.composition.ForgeDialogueCompositionRoot;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Thin Forge entrypoint that delegates lifecycle wiring to the composition root.
 */
@Mod(EchoDialogueMod.MOD_ID)
public final class EchoDialogueMod {
    public static final String MOD_ID = "echo_dialogue";

    private final ForgeDialogueCompositionRoot compositionRoot;

    public EchoDialogueMod(FMLJavaModLoadingContext context) {
        this.compositionRoot = new ForgeDialogueCompositionRoot();
        this.compositionRoot.register(context.getModEventBus(), MinecraftForge.EVENT_BUS);
    }
}
