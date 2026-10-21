package io.github.kxyr0.echoobservation.authoring.forge;

import io.github.kxyr0.echoobservation.authoring.forge.client.ObservationAuthoringBootstrap;
import io.github.kxyr0.echoobservation.authoring.forge.config.ObservationAuthoringConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoObservationAuthoringMod.MOD_ID)
public final class EchoObservationAuthoringMod {
    public static final String MOD_ID = "echo_observation_authoring";

    public EchoObservationAuthoringMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ObservationAuthoringConfig.SPEC,
                "echo-observation-authoring-client.toml");
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> ObservationAuthoringBootstrap.register(modBus));
    }
}
