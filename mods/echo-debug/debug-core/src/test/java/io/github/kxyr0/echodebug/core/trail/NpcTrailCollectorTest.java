package io.github.kxyr0.echodebug.core.trail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NpcTrailCollectorTest {
    private static final NpcTrailConfig CONFIG = new NpcTrailConfig(0.15D, 10L, 4, 3);

    @Test
    void firstPointIsRecorded() {
        NpcTrailCollector collector = new NpcTrailCollector();

        assertTrue(collector.accept(event("move-a", point(0.0D, 64.0D, 0.0D, 1L), 1L), CONFIG));

        assertEquals(1, collector.snapshot().get(0).points().size());
    }

    @Test
    void movementBelowThresholdIsSkipped() {
        NpcTrailCollector collector = seeded();

        assertFalse(collector.accept(event("move-a", point(0.05D, 64.0D, 0.0D, 2L), 2L), CONFIG));

        assertEquals(1, collector.snapshot().get(0).points().size());
        assertEquals(1L, collector.pointsRejectedByDistance());
        assertEquals(1L, collector.pointsRejectedByInterval());
    }

    @Test
    void movementAboveThresholdIsRecorded() {
        NpcTrailCollector collector = seeded();

        assertTrue(collector.accept(event("move-a", point(0.20D, 64.0D, 0.0D, 2L), 2L), CONFIG));

        assertEquals(2, collector.snapshot().get(0).points().size());
    }

    @Test
    void maximumIntervalRecordsStationaryPoint() {
        NpcTrailCollector collector = seeded();

        assertTrue(collector.accept(event("move-a", point(0.0D, 64.0D, 0.0D, 11L), 2L), CONFIG));

        assertEquals(2, collector.snapshot().get(0).points().size());
    }

    @Test
    void terminalPointIsRecordedAndKeepsY() {
        NpcTrailCollector collector = seeded();

        collector.accept(new NpcTrailEvent("move-a", "npc-a", "minecraft:overworld",
                new NpcTrailPoint(0.0D, 65.25D, 0.0D, 2L), NpcTrailStatus.ARRIVED, 2L), CONFIG);

        NpcMovementTrail trail = collector.snapshot().get(0);
        assertEquals(NpcTrailStatus.ARRIVED, trail.status());
        assertEquals(65.25D, trail.points().get(1).y());
    }

    @Test
    void staleRevisionIsIgnored() {
        NpcTrailCollector collector = seeded();
        collector.accept(event("move-a", point(1.0D, 64.0D, 0.0D, 2L), 4L), CONFIG);

        assertFalse(collector.accept(event("move-a", point(2.0D, 64.0D, 0.0D, 3L), 3L), CONFIG));

        assertEquals(2, collector.snapshot().get(0).points().size());
        assertEquals(1L, collector.staleSnapshotsRejected());
    }

    @Test
    void acceptedPointCounterTracksRecordedSamples() {
        NpcTrailCollector collector = seeded();

        collector.accept(event("move-a", point(1.0D, 64.0D, 0.0D, 2L), 2L), CONFIG);

        assertEquals(2L, collector.pointsAccepted());
        assertTrue(collector.lastDiagnostic().contains("accepted point"));
    }

    @Test
    void pointLimitDropsOldestPoint() {
        NpcTrailCollector collector = seeded();
        collector.accept(event("move-a", point(1.0D, 64.0D, 0.0D, 2L), 2L), CONFIG);
        collector.accept(event("move-a", point(2.0D, 64.0D, 0.0D, 3L), 3L), CONFIG);
        collector.accept(event("move-a", point(3.0D, 64.0D, 0.0D, 4L), 4L), CONFIG);
        collector.accept(event("move-a", point(4.0D, 64.0D, 0.0D, 5L), 5L), CONFIG);

        NpcMovementTrail trail = collector.snapshot().get(0);
        assertEquals(4, trail.points().size());
        assertEquals(1.0D, trail.points().get(0).x());
    }

    @Test
    void maxTrailsEvictsOldestTerminalBeforeActive() {
        NpcTrailCollector collector = new NpcTrailCollector();
        collector.accept(terminal("terminal-a", 1L), CONFIG);
        collector.accept(event("active-a", point(1.0D, 64.0D, 0.0D, 1L), 1L), CONFIG);
        collector.accept(terminal("terminal-b", 1L), CONFIG);

        collector.accept(event("active-b", point(2.0D, 64.0D, 0.0D, 1L), 1L), CONFIG);

        assertEquals(3, collector.trailCount());
        assertFalse(collector.snapshot().stream().anyMatch(trail -> trail.moveId().equals("terminal-a")));
        assertTrue(collector.snapshot().stream().anyMatch(trail -> trail.moveId().equals("active-a")));
    }

    @Test
    void clearAllReturnsTrailCount() {
        NpcTrailCollector collector = seeded();
        collector.accept(event("move-b", point(1.0D, 64.0D, 0.0D, 1L), 1L), CONFIG);

        assertEquals(2, collector.clearAll());
        assertEquals(0, collector.trailCount());
    }

    private static NpcTrailCollector seeded() {
        NpcTrailCollector collector = new NpcTrailCollector();
        collector.accept(event("move-a", point(0.0D, 64.0D, 0.0D, 1L), 1L), CONFIG);
        return collector;
    }

    private static NpcTrailEvent terminal(String moveId, long revision) {
        return new NpcTrailEvent(moveId, "npc-a", "minecraft:overworld", new NpcTrailPoint(0.0D, 64.0D, 0.0D,
                revision), NpcTrailStatus.ARRIVED, revision);
    }

    private static NpcTrailEvent event(String moveId, NpcTrailPoint point, long revision) {
        return new NpcTrailEvent(moveId, "npc-a", "minecraft:overworld", point, NpcTrailStatus.ACTIVE, revision);
    }

    private static NpcTrailPoint point(double x, double y, double z, long tick) {
        return new NpcTrailPoint(x, y, z, tick);
    }
}
