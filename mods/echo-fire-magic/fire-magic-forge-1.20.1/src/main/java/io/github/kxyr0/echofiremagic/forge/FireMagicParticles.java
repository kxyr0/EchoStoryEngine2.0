package io.github.kxyr0.echofiremagic.forge;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class FireMagicParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, EchoFireMagicMod.MOD_ID);
    public static final RegistryObject<MagicBillboardParticleType> FIRE_FLAME = PARTICLES.register(
            "fire_flame", MagicBillboardParticleType::new);

    private FireMagicParticles() { }

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}

