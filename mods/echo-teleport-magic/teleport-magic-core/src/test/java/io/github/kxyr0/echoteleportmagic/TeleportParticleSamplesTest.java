package io.github.kxyr0.echoteleportmagic;

import io.github.kxyr0.echoteleportmagic.visual.TeleportParticleField;
import io.github.kxyr0.echoteleportmagic.visual.TeleportParticleSamples;
import io.github.kxyr0.echoteleportmagic.timeline.TeleportTimeline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

final class TeleportParticleSamplesTest {
    @Test void longerBuildupKeepsBurstAtCommitAndPreservesResidualDuration() {
        Assertions.assertEquals(47, TeleportTimeline.COMMIT_TICK);
        Assertions.assertEquals(1.95, TeleportTimeline.visualTime(2.35), .00001);
        Assertions.assertTrue(TeleportTimeline.visualTime(1.95) < 1.95);
        Assertions.assertEquals(2.45, TeleportTimeline.visualTime(2.85), .00001);
        Assertions.assertEquals(4, TeleportTimeline.visualTime(4.4), .00001);
    }
    @Test void samplesMatchAnalyticalMotionAndSupportFreezeRewindAndCommit() {
        var field = new TeleportParticleField(173, .6f, 1.8f, false);
        var samples = new TeleportParticleSamples(field);
        double[] times = {0, .025, .3, .325, 1.90, 1.95, 1.975, 2, 2.025, 3.8, .825, .825, 4};
        for (double t : times) {
            samples.update(t);
            for (int i = 0; i < field.particles().size(); i++) {
                var orb = field.particles().get(i);
                double turn = orb.orbit(t), c = Math.cos(turn), s = Math.sin(turn), travel = orb.radialTravel(t);
                Assertions.assertEquals((orb.x() * c - orb.z() * s) + (orb.vx() * c - orb.vz() * s) * travel, samples.value(i, 0), .008);
                Assertions.assertEquals(orb.y() + orb.vy() * orb.upTravel(t), samples.value(i, 1), .008);
                Assertions.assertEquals((orb.x() * s + orb.z() * c) + (orb.vx() * s + orb.vz() * c) * travel, samples.value(i, 2), .008);
                double size = orb.size() * (1 + Math.max(0, Math.min(t - orb.birth(), 1.2)) * .12);
                Assertions.assertEquals(size, samples.value(i, 3), .001);
            }
        }
    }
    @Test void fasterSpawningStartsImmediatelyKeepsBurstAndLeavesRoomForResidualSparks() {
        var particles = new TeleportParticleField(173, .6f, 1.8f, false).particles();
        Assertions.assertEquals(5, particles.stream().filter(p -> p.birth() == 0).count());
        Assertions.assertTrue(particles.stream().filter(p -> p.birth() <= .3).count() >= 15);
        Assertions.assertTrue(particles.stream().filter(p -> p.birth() >= 1.95 && p.birth() < 2).count() >= 96);
        Assertions.assertTrue(particles.stream().anyMatch(p -> p.birth() > 2.9 && p.kind() == 2));
        Assertions.assertTrue(particles.size() <= TeleportParticleField.BUDGET);
    }
}
