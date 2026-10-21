package io.github.kxyr0.echosparkmagic.forge;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SparkMagicParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, EchoSparkMagicMod.MOD_ID);
    public static final RegistryObject<MagicBillboardParticleType> SPARK_CIRCLE =
            PARTICLES.register("spark_circle", MagicBillboardParticleType::new);

    private SparkMagicParticles() { }

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}

