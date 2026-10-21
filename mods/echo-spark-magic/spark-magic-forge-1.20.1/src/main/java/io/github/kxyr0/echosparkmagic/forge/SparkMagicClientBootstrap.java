package io.github.kxyr0.echosparkmagic.forge;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = EchoSparkMagicMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class SparkMagicClientBootstrap {
    private SparkMagicClientBootstrap() { }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(SparkMagicParticles.SPARK_CIRCLE.get(),
                MagicBillboardParticleProvider::new);
    }
}

