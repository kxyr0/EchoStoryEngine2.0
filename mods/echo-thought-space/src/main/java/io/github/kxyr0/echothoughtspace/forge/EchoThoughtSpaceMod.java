package io.github.kxyr0.echothoughtspace.forge;

import io.github.kxyr0.echothoughtspace.forge.client.ThoughtSpaceClientBootstrap;
import io.github.kxyr0.echothoughtspace.forge.config.ThoughtSpaceClientConfig;
import io.github.kxyr0.echothoughtspace.integration.observation.ObservationThoughtAdapterBootstrap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoThoughtSpaceMod.MOD_ID)
public final class EchoThoughtSpaceMod {
    public static final String MOD_ID = "echo_thought_space";

    public EchoThoughtSpaceMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ThoughtSpaceClientConfig.SPEC,
                "echo-thought-space-client.toml");
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> {
            ThoughtSpaceClientBootstrap.register(modBus);
            if (ModList.get().isLoaded("echo_observation")) ObservationThoughtAdapterBootstrap.register();
        });
    }
}

