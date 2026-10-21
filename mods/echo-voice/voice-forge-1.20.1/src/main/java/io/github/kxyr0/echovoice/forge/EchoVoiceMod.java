package io.github.kxyr0.echovoice.forge;

import io.github.kxyr0.echovoice.forge.client.VoiceClientBootstrap;
import io.github.kxyr0.echovoice.forge.config.EchoVoiceClientConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoVoiceMod.MOD_ID)
public final class EchoVoiceMod {
    public static final String MOD_ID = "echo_voice";

    public EchoVoiceMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, EchoVoiceClientConfig.SPEC,
                "echo-voice-client.toml");
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                VoiceClientBootstrap.bootstrap(FMLJavaModLoadingContext.get().getModEventBus()));
    }
}
