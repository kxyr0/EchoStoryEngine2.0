package io.github.kxyr0.echovoidmagic.forge;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class VoidMagicParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, EchoVoidMagicMod.MOD_ID);
    public static final RegistryObject<MagicBillboardParticleType> VOID_CIRCLE = PARTICLES.register(
            "void_circle", MagicBillboardParticleType::new);
    private VoidMagicParticles() { }
    public static void register(IEventBus bus) { PARTICLES.register(bus); }
}

