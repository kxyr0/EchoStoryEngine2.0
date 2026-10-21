package io.github.kxyr0.echoteleportmagic;

import io.github.kxyr0.echoteleportmagic.api.*;
import io.github.kxyr0.echoteleportmagic.runtime.*;
import io.github.kxyr0.echoteleportmagic.timeline.TeleportTimeline;
import io.github.kxyr0.echoteleportmagic.visual.TeleportParticleField;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class TeleportRuntimeTest {
    @ParameterizedTest @CsvSource({"0.1,PRECHARGE", ".8,BUILDUP", "1.4,SURGE", "1.9,CONCEALMENT",
            "2.4,AFTERBURST", "3.3,RESIDUAL", "4,COMPLETE"})
    void referencePhases(double t, TeleportTimeline.Phase phase) { assertEquals(phase, TeleportTimeline.phase(t)); }

    @Test void commitOnceUnderDuplicateDispatchAndSkippedTicks() {
        Port port = new Port(); var runtime = new TeleportCoordinator(port); var e = execution(false);
        runtime.start(e); runtime.start(e);
        runtime.tick(39); assertEquals(0, port.commits);
        runtime.tick(TeleportTimeline.COMMIT_TICK - 1); assertEquals(0, port.commits); assertTrue(port.seated);
        runtime.tick(TeleportTimeline.COMMIT_TICK); assertEquals(1, port.commits); assertFalse(port.seated);
        for (int i = 0; i < 100; i++) runtime.tick(TeleportTimeline.COMMIT_TICK + 1);
        assertEquals(1, port.commits); assertEquals(1, port.starts);
        runtime.tick(90); assertEquals(TeleportState.COMPLETED, port.terminal.state());
        assertTrue(port.terminal.committed()); assertTrue(runtime.active().isEmpty());
    }
    @Test void targetInvalidFailsBeforeAnyVisibleSideEffect() {
        Port port = new Port(); port.valid = false;
        assertThrows(IllegalArgumentException.class, () -> new TeleportCoordinator(port).start(execution(false)));
        assertEquals(0, port.starts); assertEquals(0, port.commits); assertTrue(port.receipts.isEmpty());
    }
    @Test void previewPreservesSeatAndDoesNotTouchGameplayOrPersistentReceipts() {
        Port port = new Port(); var runtime = new TeleportCoordinator(port);
        runtime.start(execution(true)); runtime.tick(TeleportTimeline.COMMIT_TICK); runtime.tick(TeleportTimeline.COMPLETE_TICK);
        assertEquals(0, port.commits); assertTrue(port.seated); assertTrue(port.receipts.isEmpty());
    }
    @Test void disconnectBeforeCommitCancelsAndAfterCommitNeverRollsBack() {
        Port port = new Port(); var runtime = new TeleportCoordinator(port); var e = execution(false);
        runtime.start(e); port.alive = false; runtime.tick(20);
        assertEquals(TeleportState.CANCELLED, port.terminal.state()); assertEquals(0, port.commits);
        port = new Port(); runtime = new TeleportCoordinator(port); e = execution(false);
        runtime.start(e); runtime.tick(TeleportTimeline.COMMIT_TICK); runtime.cancel(e.id());
        assertEquals(1, port.commits); assertTrue(port.terminal.committed());
        assertEquals(TeleportPort.ReceiptStage.AFTER_COMMIT, port.receipt(e.id()));
    }
    @Test void recoveryAfterCommitNeverReplaysMutation() {
        Port port = new Port(); var e = execution(false);
        var runtime = new TeleportCoordinator(port); runtime.start(e); runtime.tick(TeleportTimeline.COMMIT_TICK);
        var recovered = new TeleportCoordinator(port).start(e);
        assertEquals(TeleportState.COMPLETED, recovered.state()); assertEquals(1, port.commits);
    }
    @Test void ambiguousWriteAheadIntentIsNeverReplayed() {
        Port port = new Port(); var e = execution(false);
        port.receipts.put(e.id(), TeleportPort.ReceiptStage.COMMIT_INTENT);
        assertThrows(IllegalStateException.class, () -> new TeleportCoordinator(port).start(e));
        assertEquals(0, port.commits); assertEquals(0, port.starts);
    }
    @Test void cancellationOwnedDoesNotTouchIndependentExecutions() {
        Port port = new Port(); var runtime = new TeleportCoordinator(port);
        var a = execution(false); runtime.start(a);
        var b = execution(false); runtime.start(new TeleportExecution(b.id(), b.subject(), b.source(), b.destination(),
                0, b.seed(), b.state(), false, false, "other", b.provenance(), 1));
        runtime.cancelOwned("scene"); assertEquals(1, runtime.active().size());
        runtime.tick(TeleportTimeline.COMMIT_TICK); assertEquals(1, port.commits);
    }
    @Test void movingAnchorFollowsThenFreezesAtCommit() {
        Port port = new Port(); var runtime = new TeleportCoordinator(port); var e = execution(false);
        runtime.start(e); port.x = 5; runtime.tick(30); port.x = 9; runtime.tick(TeleportTimeline.COMMIT_TICK);
        port.x = 500; runtime.tick(55);
        assertEquals(9, runtime.find(e.id()).orElseThrow().source().x());
    }
    @Test void subjectCannotRunTwoConcurrentExecutions() {
        Port port = new Port(); var runtime = new TeleportCoordinator(port); var a = execution(false);
        runtime.start(a);
        var b = new TeleportExecution(TeleportExecutionId.random(), a.subject(), a.source(), a.destination(),
                0, 22, a.state(), false, false, "other", "test", 1);
        assertThrows(IllegalStateException.class, () -> runtime.start(b));
    }
    @Test void deterministicFieldIsBoundedUpwardAndExpiresByFourSeconds() {
        var a = new TeleportParticleField(173, .6f, 1.17f, false);
        var b = new TeleportParticleField(173, .6f, 1.17f, false);
        assertEquals(a.particles(), b.particles());
        assertTrue(a.particles().size() <= TeleportParticleField.BUDGET);
        assertTrue(a.peakAlive() > 100);
        assertTrue(a.particles().stream().allMatch(p -> p.vy() > 0 && p.birth() + p.life() <= 4));
        assertTrue(a.particles().stream().noneMatch(p -> p.alive(4)));
        assertTrue(a.particles().stream().filter(p -> p.birth() > 2.9).allMatch(p -> p.kind() == 2));
        System.out.println("Reference particle field: total=" + a.particles().size() + ", peakAlive=" + a.peakAlive());
    }
    @Test void particlesOrbitWithoutInwardMotionAccumulateAndBurstAtCommit() {
        var field = new TeleportParticleField(173, .6f, 1.17f, false);
        var early = field.particles().stream().filter(p -> p.birth() == 0).findFirst().orElseThrow();
        assertTrue(early.alive(1.95));
        assertTrue(early.alpha(1.85) > .3 && early.alpha(1.85) <= .6);
        assertEquals(0, early.upTravel(1.95));
        assertEquals(0, early.radialTravel(1.95));
        assertTrue(early.upTravel(2.45) > .6);
        assertTrue(early.radialTravel(2.45) > 4);
        assertTrue(early.radialTravel(1.96) > .14);
        assertTrue(field.particles().stream().allMatch(p -> p.x() * p.vx() + p.z() * p.vz() > 0));
        assertEquals(4, field.particles().stream().map(p -> (p.vx() > 0 ? 1 : 0) + (p.vz() > 0 ? 2 : 0)).distinct().count());
        assertTrue(field.particles().stream().allMatch(p -> Math.hypot(p.x(), p.z()) >= .48 * 1.12));
        long close = field.particles().stream().filter(p -> Math.hypot(p.x(), p.z()) < .555 * 1.12).count();
        assertTrue(close > field.particles().size() * .65);
        assertTrue(close < field.particles().size() * .9);
        assertTrue(Math.abs(early.orbit(.8)) > .3);
        assertNotEquals(early.orbit(1.5), early.orbit(1.9));
        assertEquals(early.orbit(1.95), early.orbit(2.05));
        long before = field.particles().stream().filter(p -> p.alive(1.94)).count();
        long burst = field.particles().stream().filter(p -> p.alive(1.96)).count();
        assertTrue(burst >= before + 96);
        assertTrue(before > field.particles().stream().filter(p -> p.alive(.8)).count() * 2);
    }
    @Test void particlesSpawnAlongTheBodyAndAboveTheHeadFromTheFirstBatch() {
        float height = 1.8f;
        var particles = new TeleportParticleField(173, .6f, height, false).particles();
        assertTrue(particles.get(0).y() < height * .45);
        assertTrue(particles.get(1).y() >= height * .45);
        assertTrue(particles.get(2).y() >= height * .85);
        long aboveHead = particles.stream().filter(p -> p.y() > height).count();
        assertTrue(aboveHead > particles.size() * .15);
        assertTrue(particles.stream().allMatch(p -> p.y() >= height * .05 && p.y() <= height * 2.15));
        assertTrue(particles.stream().anyMatch(p -> p.birth() >= 1.8 && p.y() > height * 1.8));
    }
    @Test void concealmentCoversTheCommitAndOutlivesTheSubject() {
        assertTrue(TeleportTimeline.flash(1.95) > .99);
        assertTrue(TeleportTimeline.flash(2.15) > .99);
        assertTrue(TeleportTimeline.ground(3) > .5);
        assertEquals(0, TeleportTimeline.ground(4));
    }
    @Test void queuedNpcCommitWaitsForAcknowledgementAndKeepsFullResidualDuration() {
        Port port = new Port(); port.pendingTicks = 3;
        var runtime = new TeleportCoordinator(port); var e = execution(false);
        runtime.start(e); runtime.tick(TeleportTimeline.COMMIT_TICK); runtime.tick(TeleportTimeline.COMMIT_TICK + 1); runtime.tick(TeleportTimeline.COMMIT_TICK + 2);
        assertEquals(0, port.commits);
        assertEquals(TeleportPort.ReceiptStage.COMMIT_INTENT, port.receipt(e.id()));
        runtime.tick(TeleportTimeline.COMMIT_TICK + 3); assertEquals(1, port.commits);
        runtime.tick(TeleportTimeline.COMPLETE_TICK); assertEquals(1, runtime.active().size());
        runtime.tick(TeleportTimeline.COMPLETE_TICK + 3); assertTrue(runtime.active().isEmpty());
    }
    @Test void cancellationDuringNpcWriteWaitsForItsIrreversibleCommit() {
        Port port = new Port(); port.pendingTicks = 1;
        var runtime = new TeleportCoordinator(port); var e = execution(false);
        runtime.start(e); runtime.tick(TeleportTimeline.COMMIT_TICK); runtime.cancel(e.id());
        assertEquals(1, runtime.active().size());
        runtime.tick(TeleportTimeline.COMMIT_TICK + 1); assertEquals(1, port.commits);
        assertEquals(TeleportState.CANCELLED, port.terminal.state());
        assertTrue(port.terminal.committed());
        assertEquals(TeleportPort.ReceiptStage.AFTER_COMMIT, port.receipt(e.id()));
    }
    static TeleportExecution execution(boolean preview) {
        return new TeleportExecution(TeleportExecutionId.random(), UUID.randomUUID(),
                new TeleportAnchor(0, 64, 0, 0, .6f, 1.17f),
                new TeleportDestination("jdh:door", "minecraft:overworld", 20, 64, 20, 0, 0),
                0, 173, TeleportState.CREATED, false, preview, "scene", "test.es:3", 1);
    }
    static final class Port implements TeleportPort {
        boolean valid = true, alive = true, seated = true;
        int commits, starts, pendingTicks;
        double x;
        TeleportExecution terminal;
        final Map<TeleportExecutionId, ReceiptStage> receipts = new HashMap<>();
        public void prepare(TeleportExecution e) { if (!valid) throw new IllegalArgumentException("Invalid target"); }
        public boolean alive(TeleportExecution e) { return alive; }
        public TeleportAnchor anchor(TeleportExecution e) { return new TeleportAnchor(x, 64, 0, 0, .6f, 1.17f); }
        public void visualStart(TeleportExecution e) { starts++; }
        public boolean commit(TeleportExecution e) {
            assertEquals(ReceiptStage.COMMIT_INTENT, receipt(e.id()));
            if (pendingTicks-- > 0) return false;
            commits++; seated = false; return true;
        }
        public void visualCommit(TeleportExecution e) { }
        public void finished(TeleportExecution e) { terminal = e; }
        public void receipt(TeleportExecution e, ReceiptStage s) { if (!e.preview()) receipts.put(e.id(), s); }
        public ReceiptStage receipt(TeleportExecutionId id) { return receipts.getOrDefault(id, ReceiptStage.NONE); }
    }
}
