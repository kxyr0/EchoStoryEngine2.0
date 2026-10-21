package io.github.kxyr0.echobloodmagic.forge;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BloodMagicParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, EchoBloodMagicMod.MOD_ID);
    public static final RegistryObject<MagicBillboardParticleType> BLOOD_CIRCLE =
            PARTICLES.register("blood_circle", MagicBillboardParticleType::new);

    private BloodMagicParticles() { }

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}

