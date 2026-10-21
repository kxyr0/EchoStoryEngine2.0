package io.github.kxyr0.echoteleportmagic.visual;

import io.github.kxyr0.echoteleportmagic.timeline.TeleportTimeline;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Spawn parameters sampled once. Motion and fade are analytical and independent of frame rate. */
public final class TeleportParticleField {
    public static final int BUDGET = 512;
    private final List<Orb> particles;
    public TeleportParticleField(long seed, float width, float height, boolean arrival) {
        Random random = new Random(seed);
        var result = new ArrayList<Orb>();
        for (int tick = 0; tick < (arrival ? 26 : 66); tick++) {
            double t = tick * .05;
            float envelope = arrival ? TeleportTimeline.arrival(t) : TeleportTimeline.emission(t);
            int count = tick == 0 ? 5 : Math.round((arrival || t < 2.1 ? 13 : 7) * envelope);
            if (!arrival && tick < TeleportTimeline.VISUAL_COMMIT_TICK) count = Math.max(2, count);
            if (!arrival && tick == TeleportTimeline.VISUAL_COMMIT_TICK) count += 96;
            for (int i = 0; i < count && result.size() < BUDGET; i++) {
                result.add(sample(random, t, width, height, arrival, result.size() % 3));
            }
        }
        particles = List.copyOf(result);
    }
    private Orb sample(Random r, double birth, float width, float height, boolean arrival, int heightBand) {
        int kind = birth < .30 && !arrival ? 0 : birth > 2.9 ? 2 : r.nextInt(100) < 34 ? 1 : r.nextInt(3) == 0 ? 2 : 0;
        double angle = r.nextDouble() * Math.PI * 2;
        float scale = Math.max(width, height * .42f);
        double life = kind == 0 ? .8 + r.nextDouble() : kind == 1 ? .45 + r.nextDouble() * .75 : .4 + r.nextDouble() * .5;
        if (!arrival && birth <= 1.95) {
            double residual = kind == 0 ? .75 + r.nextDouble() * .95 : kind == 1 ? .4 + r.nextDouble() * .7 : .35 + r.nextDouble() * .55;
            life = 1.95 - birth + residual;
        }
        life = Math.min(life, (arrival ? 1.65 : 4) - birth);
        float size = (kind == 0 ? .05f + r.nextFloat() * .22f : kind == 1 ? .04f + r.nextFloat() * .14f : .018f + r.nextFloat() * .03f) * scale * 1.12f;
        boolean nearBody = r.nextDouble() < .78;
        double radius = width * .5 + .18 + r.nextDouble() * scale * (nearBody ? .14 : .55);
        if (!nearBody) radius += scale * .22;
        radius *= 1.12;
        double columnHeight = 1.2 + .9 * TeleportTimeline.smooth(.3, 1.95, birth);
        double spawnY = height * (.05 + (heightBand + r.nextDouble()) * columnHeight / 3);
        double launchAngle = angle + (r.nextDouble() - .5) * 1.3;
        double launchSpeed = (.25 + r.nextDouble() * .55) * scale;
        return new Orb(birth, life, Math.cos(angle) * radius, spawnY, Math.sin(angle) * radius,
                Math.cos(launchAngle) * launchSpeed,
                (.15 + r.nextDouble() * (kind == 2 ? 1.2 : .85)) * height,
                Math.sin(launchAngle) * launchSpeed,
                size, kind, r.nextFloat() * 6.28f, .75f + r.nextFloat() * .25f, !arrival, height);
    }
    public List<Orb> particles() { return particles; }
    public int peakAlive() {
        int peak = 0;
        for (double t = 0; t < 4; t += .01) {
            int count = 0;
            for (Orb orb : particles) if (orb.alive(t)) count++;
            peak = Math.max(peak, count);
        }
        return peak;
    }
    public record Orb(double birth, double life, double x, double y, double z,
            double vx, double vy, double vz, float size, int kind, float drift, float brightness, boolean departure, float height) {
        public boolean alive(double t) { return t >= birth && t < birth + life; }
        public float alpha(double t) {
            double age = t - birth;
            double fadeStart = departure ? Math.max(life * .45, Math.min(life - .15, 1.95 - birth)) : life * .45;
            float charge = departure ? .32f + .28f * TeleportTimeline.smooth(1.5, 1.95, t) : .5f;
            return TeleportTimeline.smooth(0, .08, age) * (1 - TeleportTimeline.smooth(fadeStart, life, age)) * brightness * charge;
        }
        public double upTravel(double t) { return travel(t, 2.4); }
        public double radialTravel(double t) { return travel(t, 16); }
        public double orbit(double t) {
            if (!departure) return 0;
            return Math.max(0, Math.min(t, 1.95) - birth) * (.7 + .25 * Math.sin(drift)) * (drift > Math.PI ? 1 : -1);
        }
        private double travel(double t, double releasedSpeed) {
            double age = Math.max(0, t - birth);
            if (!departure) return age;
            double released = Math.max(0, t - Math.max(1.95, birth));
            return releasedSpeed * (1 - Math.exp(-released * 2)) / 2;
        }
    }
}
