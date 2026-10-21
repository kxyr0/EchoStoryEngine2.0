package io.github.kxyr0.echomagic.forge.api.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;

public final class MagicBillboardParticleProvider implements ParticleProvider<MagicBillboardParticleOptions> {
    private final SpriteSet sprites;
    private final MagicParticleMotion motion;
    public MagicBillboardParticleProvider(SpriteSet sprites) { this(sprites, MagicParticleMotion.NONE); }
    public MagicBillboardParticleProvider(SpriteSet sprites, MagicParticleMotion motion) {
        this.sprites = sprites;
        this.motion = motion;
    }
    @Override public Particle createParticle(MagicBillboardParticleOptions options, ClientLevel level,
            double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        MagicBillboardParticle particle = new MagicBillboardParticle(level, x, y, z, options);
        particle.setParticleSpeed(xSpeed + motion.x(), ySpeed + motion.y(), zSpeed + motion.z());
        particle.pickSprite(sprites);
        return particle;
    }
}
