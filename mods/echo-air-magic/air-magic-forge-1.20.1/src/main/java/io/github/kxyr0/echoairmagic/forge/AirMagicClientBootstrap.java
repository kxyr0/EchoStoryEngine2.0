package io.github.kxyr0.echoairmagic.forge;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleProvider;
import io.github.kxyr0.echomagic.forge.api.client.particle.MagicParticleMotion;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = EchoAirMagicMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class AirMagicClientBootstrap {
    private static final double AIR_RISE_PER_TICK = 0.025D;

    private AirMagicClientBootstrap() { }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(AirMagicParticles.AIR_CIRCLE.get(),
                sprites -> new MagicBillboardParticleProvider(sprites, airMotion()));
    }

    static MagicParticleMotion airMotion() {
        return new MagicParticleMotion(0.0D, AIR_RISE_PER_TICK, 0.0D);
    }
}

