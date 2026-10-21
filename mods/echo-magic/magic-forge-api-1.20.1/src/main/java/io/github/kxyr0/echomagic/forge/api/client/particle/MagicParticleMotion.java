package io.github.kxyr0.echomagic.forge.api.client.particle;

public record MagicParticleMotion(double x, double y, double z) {
    public static final MagicParticleMotion NONE = new MagicParticleMotion(0.0D, 0.0D, 0.0D);

    public MagicParticleMotion {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Particle motion must be finite.");
        }
    }
}
