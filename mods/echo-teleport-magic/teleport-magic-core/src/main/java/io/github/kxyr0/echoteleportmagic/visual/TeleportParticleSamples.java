package io.github.kxyr0.echoteleportmagic.visual;

/** Two reusable 20 Hz samples. Render frames only interpolate; no per-particle frame allocations. */
public final class TeleportParticleSamples {
    public static final int STRIDE = 4;
    private final TeleportParticleField field;
    private float[] previous, next;
    private int tick = Integer.MIN_VALUE;
    private float fraction;

    public TeleportParticleSamples(TeleportParticleField field) {
        this.field = field;
        previous = new float[field.particles().size() * STRIDE];
        next = new float[previous.length];
    }
    public void update(double time) {
        int requested = (int) Math.floor(time * 20);
        fraction = (float) (time * 20 - requested);
        if (requested == tick) return;
        if (requested == tick + 1) {
            float[] swap = previous; previous = next; next = swap;
        } else sample(previous, requested * .05);
        sample(next, (requested + 1) * .05);
        tick = requested;
    }
    private void sample(float[] target, double time) {
        var particles = field.particles();
        for (int i = 0; i < particles.size(); i++) {
            var orb = particles.get(i);
            int index = i * STRIDE;
            double turn = orb.orbit(time), cosine = Math.cos(turn), sine = Math.sin(turn);
            double radial = orb.radialTravel(time);
            target[index] = (float) ((orb.x() * cosine - orb.z() * sine) + (orb.vx() * cosine - orb.vz() * sine) * radial);
            target[index + 1] = (float) (orb.y() + orb.vy() * radial * (orb.departure() ? .15 : 1));
            target[index + 2] = (float) ((orb.x() * sine + orb.z() * cosine) + (orb.vx() * sine + orb.vz() * cosine) * radial);
            target[index + 3] = orb.size() * (1 + (float) Math.max(0, Math.min(time - orb.birth(), 1.2)) * .12f);
        }
    }
    public float value(int particle, int component) {
        int index = particle * STRIDE + component;
        return previous[index] + (next[index] - previous[index]) * fraction;
    }
}
