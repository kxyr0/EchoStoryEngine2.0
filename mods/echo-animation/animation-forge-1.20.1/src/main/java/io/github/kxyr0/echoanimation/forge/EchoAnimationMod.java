package io.github.kxyr0.echoanimation.forge;

import io.github.kxyr0.echoanimation.forge.composition.ForgeAnimationCompositionRoot;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Thin Forge entrypoint for Echo Animation.
 */
@Mod(EchoAnimationMod.MOD_ID)
public final class EchoAnimationMod {
    public static final String MOD_ID = "echo_animation";

    private final ForgeAnimationCompositionRoot compositionRoot;

    public EchoAnimationMod(FMLJavaModLoadingContext context) {
        this.compositionRoot = new ForgeAnimationCompositionRoot();
        this.compositionRoot.register(context.getModEventBus(), MinecraftForge.EVENT_BUS);
    }
}

