package io.github.kxyr0.echovisibility.api.debug;

public record VisibilityPerformanceSnapshot(
        int activeWatches,
        long renderedFrames,
        long frustumTests,
        long occlusionRays,
        long reportsSent,
        long duplicateReports,
        long evaluationNanos,
        long fullWorldScans) {
}
