package io.github.kxyr0.echofiremagic.forge;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleProvider;
import io.github.kxyr0.echomagic.forge.api.client.particle.MagicParticleMotion;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = EchoFireMagicMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class FireMagicClientBootstrap {
    private static final double FLAME_RISE_PER_TICK = 0.04D;

    private FireMagicClientBootstrap() { }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(FireMagicParticles.FIRE_FLAME.get(),
                sprites -> new MagicBillboardParticleProvider(sprites, flameMotion()));
    }

    static MagicParticleMotion flameMotion() {
        return new MagicParticleMotion(0.0D, FLAME_RISE_PER_TICK, 0.0D);
    }
}

