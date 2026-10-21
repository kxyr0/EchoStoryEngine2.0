package io.github.kxyr0.echobloodmagic.forge;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = EchoBloodMagicMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class BloodMagicClientBootstrap {
    private BloodMagicClientBootstrap() { }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(BloodMagicParticles.BLOOD_CIRCLE.get(),
                MagicBillboardParticleProvider::new);
    }
}

