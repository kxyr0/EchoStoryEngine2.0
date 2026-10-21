package io.github.kxyr0.echomagic.epicfight;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EchoMagicEpicFightParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, EchoMagicEpicFightMod.MOD_ID);
    public static final RegistryObject<MagicBillboardParticleType> WEAPON_GLOW =
            PARTICLES.register("weapon_glow", MagicBillboardParticleType::new);

    private EchoMagicEpicFightParticles() { }
    static void register(IEventBus bus) { PARTICLES.register(bus); }
}
