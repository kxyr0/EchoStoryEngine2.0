package io.github.kxyr0.echobackgroundnpc.core.navigation;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearanceId;
import io.github.kxyr0.echobackgroundnpc.api.runtime.MaterializationState;
import io.github.kxyr0.echobackgroundnpc.api.task.TaskKind;
import io.github.kxyr0.echobackgroundnpc.core.materialization.MaterializationPolicy;
import io.github.kxyr0.echobackgroundnpc.core.render.MovementSnapshot;
import io.github.kxyr0.echobackgroundnpc.core.render.MovementSyncTracker;
import io.github.kxyr0.echobackgroundnpc.core.render.PoseSampleCache;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PerformancePrimitivesTest {
    @Test
    void twentyCompatibleRequestsCauseOneRouteComputation() {
        RouteRequestQueue queue = new RouteRequestQueue();
        SharedRouteCache cache = new SharedRouteCache(32);
        var key = new SharedRoutePlan.RouteKey(1L, "jdh:dorm", "jdh:university", "human");
        for (int index = 0; index < 20; index++) queue.request(key, id(index));
        AtomicInteger computations = new AtomicInteger();
        var dispatch = queue.drain(2, value -> {
            computations.incrementAndGet();
            return Optional.of(route(value));
        }, cache);
        assertEquals(1, computations.get());
        assertEquals(20, dispatch.get(0).agents().size());
        assertEquals(0, queue.queuedPlans());
    }

    @Test
    void routeStormHonorsTwoComputationsPerTick() {
        RouteRequestQueue queue = new RouteRequestQueue();
        for (int index = 0; index < 50; index++) {
            queue.request(new SharedRoutePlan.RouteKey(1, "start_" + index, "target", "human"), id(index));
        }
        AtomicInteger computations = new AtomicInteger();
        queue.drain(2, key -> { computations.incrementAndGet(); return Optional.of(route(key)); },
                new SharedRouteCache(64));
        assertEquals(2, computations.get());
        assertEquals(48, queue.queuedPlans());
    }

    @Test
    void poseAndSkinEquivalentStateIsShared() {
        PoseSampleCache<float[]> cache = new PoseSampleCache<>(64);
        var key = new PoseSampleCache.PoseKey("human", 1, "walk", 4, "ACTIVE", "body");
        float[] first = cache.resolve(key, () -> new float[24]);
        float[] second = cache.resolve(key, () -> new float[24]);
        assertSame(first, second);
        assertEquals(0.5, cache.hitRate());
        assertEquals(new BackgroundAppearanceId("jdh:a"), new BackgroundAppearanceId("jdh:a"));
    }

    @Test
    void hysteresisStopsBoundaryThrashing() {
        MaterializationPolicy policy = MaterializationPolicy.defaults();
        assertEquals(MaterializationState.MATERIALIZED,
                policy.classify(MaterializationState.VIRTUAL, 63.99).materialization());
        assertEquals(MaterializationState.MATERIALIZED,
                policy.classify(MaterializationState.MATERIALIZED, 70.0).materialization());
        assertEquals(MaterializationState.VIRTUAL,
                policy.classify(MaterializationState.MATERIALIZED, 76.01).materialization());
    }

    @Test
    void stationarySnapshotsAreNotSpammed() {
        MovementSyncTracker tracker = new MovementSyncTracker(100, 0.01);
        var first = snapshot(1, 0);
        assertTrue(tracker.shouldSend(first));
        assertFalse(tracker.shouldSend(snapshot(2, 0)));
        assertTrue(tracker.shouldSend(snapshot(101, 0)));
    }

    private SharedRoutePlan route(SharedRoutePlan.RouteKey key) {
        return new SharedRoutePlan(key, List.of(new WorldPoint("minecraft:overworld", 0, 64, 0),
                new WorldPoint("minecraft:overworld", 10, 64, 0)), 10);
    }
    private MovementSnapshot snapshot(long tick, double x) {
        return new MovementSnapshot(id(1), new WorldPoint("minecraft:overworld", x, 64, 0), 0, 0, 0, 0,
                TaskKind.STAY, tick, tick);
    }
    private BackgroundNpcId id(int index) { return new BackgroundNpcId("jdh:agent_" + index); }
}
