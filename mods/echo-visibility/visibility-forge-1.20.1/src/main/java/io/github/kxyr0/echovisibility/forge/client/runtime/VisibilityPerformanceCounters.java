package io.github.kxyr0.echovisibility.forge.client.runtime;

import io.github.kxyr0.echovisibility.api.debug.VisibilityPerformanceSnapshot;

public final class VisibilityPerformanceCounters {
    private long renderedFrames;
    private long frustumTests;
    private long occlusionRays;
    private long reportsSent;
    private long duplicateReports;
    private long evaluationNanos;

    public void frame() { renderedFrames++; }
    public void evaluation(long nanos) { frustumTests++; evaluationNanos += Math.max(0L, nanos); }
    public void rays(int count) { occlusionRays += Math.max(0, count); }
    public void report() { reportsSent++; }
    public void duplicate() { duplicateReports++; }

    public VisibilityPerformanceSnapshot snapshot(int activeWatches) {
        return new VisibilityPerformanceSnapshot(activeWatches, renderedFrames, frustumTests,
                occlusionRays, reportsSent, duplicateReports, evaluationNanos, 0L);
    }
}
