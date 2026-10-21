package io.github.kxyr0.echowatermagic.forge;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = EchoWaterMagicMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class WaterMagicClientBootstrap {
    private WaterMagicClientBootstrap() { }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(WaterMagicParticles.WATER_CIRCLE.get(),
                MagicBillboardParticleProvider::new);
    }
}

