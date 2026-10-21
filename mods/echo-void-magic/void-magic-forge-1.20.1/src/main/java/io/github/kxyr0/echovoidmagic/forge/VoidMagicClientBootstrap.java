package io.github.kxyr0.echovoidmagic.forge;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EchoVoidMagicMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VoidMagicClientBootstrap {
    private VoidMagicClientBootstrap() { }
    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(VoidMagicParticles.VOID_CIRCLE.get(), MagicBillboardParticleProvider::new);
    }
}

