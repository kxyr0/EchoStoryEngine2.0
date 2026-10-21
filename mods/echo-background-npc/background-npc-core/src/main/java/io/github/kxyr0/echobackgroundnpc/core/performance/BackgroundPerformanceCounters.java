package io.github.kxyr0.echobackgroundnpc.core.performance;

import java.util.concurrent.atomic.LongAdder;

/** Low-contention counters exposed by the performance command. */
public final class BackgroundPerformanceCounters {
    private final LongAdder simulationUpdates = new LongAdder();
    private final LongAdder routeComputations = new LongAdder();
    private final LongAdder networkSnapshots = new LongAdder();
    private final LongAdder animationSamples = new LongAdder();
    private final LongAdder faceSamples = new LongAdder();

    public void simulationUpdate() { simulationUpdates.increment(); }
    public void routeComputation() { routeComputations.increment(); }
    public void networkSnapshot() { networkSnapshots.increment(); }
    public void animationSample() { animationSamples.increment(); }
    public void faceSample() { faceSamples.increment(); }
    public Snapshot snapshot(int total, int virtual, int materialized, int visible, int queuedRoutes,
            double routeHitRate, double poseHitRate, int voices) {
        return new Snapshot(total, virtual, materialized, visible, simulationUpdates.sum(), queuedRoutes,
                routeComputations.sum(), routeHitRate, networkSnapshots.sum(), animationSamples.sum(), poseHitRate,
                faceSamples.sum(), voices);
    }

    public record Snapshot(int totalAgents, int virtualAgents, int materializedAgents, int visibleAgents,
            long simulationUpdates, int queuedRoutes, long routeComputations, double routeCacheHitRate,
            long networkSnapshots, long animationSamples, double poseCacheHitRate, long faceSamples,
            int ambientVoices) { }
}
