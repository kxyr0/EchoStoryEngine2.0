package io.github.kxyr0.echomagic.epicfight.client;

import io.github.kxyr0.echomagic.epicfight.EchoMagicEpicFightMod;
import io.github.kxyr0.echomagic.epicfight.EchoMagicEpicFightParticles;
import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EchoMagicEpicFightMod.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SummonedWeaponParticleBootstrap {
    private SummonedWeaponParticleBootstrap() { }

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(EchoMagicEpicFightParticles.WEAPON_GLOW.get(),
                MagicBillboardParticleProvider::new);
    }
}
