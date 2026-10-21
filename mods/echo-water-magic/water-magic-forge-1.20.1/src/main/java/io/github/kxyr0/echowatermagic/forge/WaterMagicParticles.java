package io.github.kxyr0.echowatermagic.forge;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class WaterMagicParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, EchoWaterMagicMod.MOD_ID);
    public static final RegistryObject<MagicBillboardParticleType> WATER_CIRCLE =
            PARTICLES.register("water_circle", MagicBillboardParticleType::new);

    private WaterMagicParticles() { }

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}

