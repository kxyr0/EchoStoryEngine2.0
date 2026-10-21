package io.github.kxyr0.echomagic.forge.api.client.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;

public final class MagicBillboardParticleType extends ParticleType<MagicBillboardParticleOptions> {
    public MagicBillboardParticleType() { super(false, MagicBillboardParticleOptions.DESERIALIZER); }
    @Override public Codec<MagicBillboardParticleOptions> codec() {
        return Codec.unit(MagicBillboardParticleOptions.defaults(this));
    }
}

