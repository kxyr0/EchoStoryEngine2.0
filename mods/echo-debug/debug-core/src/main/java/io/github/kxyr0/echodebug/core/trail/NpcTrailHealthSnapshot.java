package io.github.kxyr0.echodebug.core.trail;

import java.util.Objects;

public record NpcTrailHealthSnapshot(
        boolean integrationAvailable,
        long progressEventsReceived,
        long progressSnapshotsReceived,
        long staleSnapshotsRejected,
        int trailInstances,
        long pointsAccepted,
        long pointsRejectedByDistance,
        long pointsRejectedByInterval,
        long renderSnapshotsPublished,
        int trailsInCurrentDimension,
        int trailsCulled,
        long segmentsSubmitted,
        long renderCalls,
        long lastRenderTick,
        String lastDiagnostic
) {
    public NpcTrailHealthSnapshot {
        requireNonNegative(progressEventsReceived, "progressEventsReceived");
        requireNonNegative(progressSnapshotsReceived, "progressSnapshotsReceived");
        requireNonNegative(staleSnapshotsRejected, "staleSnapshotsRejected");
        requireNonNegative(trailInstances, "trailInstances");
        requireNonNegative(pointsAccepted, "pointsAccepted");
        requireNonNegative(pointsRejectedByDistance, "pointsRejectedByDistance");
        requireNonNegative(pointsRejectedByInterval, "pointsRejectedByInterval");
        requireNonNegative(renderSnapshotsPublished, "renderSnapshotsPublished");
        requireNonNegative(trailsInCurrentDimension, "trailsInCurrentDimension");
        requireNonNegative(trailsCulled, "trailsCulled");
        requireNonNegative(segmentsSubmitted, "segmentsSubmitted");
        requireNonNegative(renderCalls, "renderCalls");
        if (lastRenderTick < -1L) {
            throw new IllegalArgumentException("lastRenderTick must be -1 or non-negative.");
        }
        lastDiagnostic = Objects.requireNonNull(lastDiagnostic, "lastDiagnostic");
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must not be negative.");
        }
    }
}
