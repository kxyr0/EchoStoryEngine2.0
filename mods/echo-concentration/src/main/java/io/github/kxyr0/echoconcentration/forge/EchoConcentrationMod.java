package io.github.kxyr0.echoconcentration.forge;

import io.github.kxyr0.echoconcentration.forge.client.ConcentrationClientBootstrap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoConcentrationMod.MOD_ID)
public final class EchoConcentrationMod {
    public static final String MOD_ID = "echo_concentration";

    public EchoConcentrationMod() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ConcentrationClientBootstrap.register(
                FMLJavaModLoadingContext.get().getModEventBus()));
    }
}
