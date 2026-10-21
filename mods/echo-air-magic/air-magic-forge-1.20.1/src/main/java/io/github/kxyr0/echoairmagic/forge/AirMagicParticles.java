package io.github.kxyr0.echoairmagic.forge;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class AirMagicParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, EchoAirMagicMod.MOD_ID);
    public static final RegistryObject<MagicBillboardParticleType> AIR_CIRCLE =
            PARTICLES.register("air_circle", MagicBillboardParticleType::new);

    private AirMagicParticles() { }

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}

