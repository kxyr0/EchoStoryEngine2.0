package io.github.kxyr0.echodebug.core.trail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class NpcTrailCollector {
    private final Map<String, MutableTrail> trails = new LinkedHashMap<>();
    private long snapshotRevision;
    private long pointsAccepted;
    private long pointsRejectedByDistance;
    private long pointsRejectedByInterval;
    private long staleSnapshotsRejected;
    private String lastDiagnostic = "collector has not received movement snapshots";

    public boolean accept(NpcTrailEvent event, NpcTrailConfig config) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(config, "config");
        MutableTrail trail = trails.get(event.moveId());
        if (trail != null && event.trailRevision() < trail.trailRevision) {
            staleSnapshotsRejected++;
            lastDiagnostic = "stale snapshot rejected moveId=" + event.moveId();
            return false;
        }
        if (trail == null) {
            createTrail(event, config);
            pointsAccepted++;
            lastDiagnostic = "created trail moveId=" + event.moveId() + " status=" + event.status();
            snapshotRevision++;
            return true;
        }
        SampleDecision decision = sampleDecision(trail, event, config);
        if (!decision.accepted()) {
            if (!decision.moved()) {
                pointsRejectedByDistance++;
            }
            if (!decision.intervalElapsed()) {
                pointsRejectedByInterval++;
            }
            trail.status = event.status();
            trail.trailRevision = event.trailRevision();
            lastDiagnostic = "sample skipped moveId=" + event.moveId();
            return false;
        }
        trail.add(event, config);
        pointsAccepted++;
        lastDiagnostic = "accepted point moveId=" + event.moveId() + " status=" + event.status();
        snapshotRevision++;
        return true;
    }

    public int clearAll() {
        int count = trails.size();
        if (count > 0) {
            trails.clear();
            snapshotRevision++;
        }
        lastDiagnostic = "cleared trails count=" + count;
        return count;
    }

    public List<NpcMovementTrail> snapshot() {
        return trails.values().stream().map(MutableTrail::snapshot).toList();
    }

    public long snapshotRevision() {
        return snapshotRevision;
    }

    public int trailCount() {
        return trails.size();
    }

    public long pointsAccepted() {
        return pointsAccepted;
    }

    public long pointsRejectedByDistance() {
        return pointsRejectedByDistance;
    }

    public long pointsRejectedByInterval() {
        return pointsRejectedByInterval;
    }

    public long staleSnapshotsRejected() {
        return staleSnapshotsRejected;
    }

    public String lastDiagnostic() {
        return lastDiagnostic;
    }

    private MutableTrail createTrail(NpcTrailEvent event, NpcTrailConfig config) {
        evictIfNeeded(config);
        MutableTrail trail = new MutableTrail(event, config);
        trails.put(event.moveId(), trail);
        return trail;
    }

    private void evictIfNeeded(NpcTrailConfig config) {
        if (trails.size() < config.maxTrails()) {
            return;
        }
        String evicted = trails.entrySet().stream()
                .filter(entry -> entry.getValue().status.terminal())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseGet(() -> trails.keySet().iterator().next());
        trails.remove(evicted);
        snapshotRevision++;
    }

    private static SampleDecision sampleDecision(MutableTrail trail, NpcTrailEvent event, NpcTrailConfig config) {
        if (event.status().terminal()) {
            return new SampleDecision(true, true, true);
        }
        NpcTrailPoint last = trail.points.get(trail.points.size() - 1);
        boolean moved = last.distanceTo(event.point()) >= config.minimumSampleDistance();
        boolean intervalElapsed = event.point().serverTick() - last.serverTick()
                >= config.maximumSampleIntervalTicks();
        return new SampleDecision(moved || intervalElapsed, moved, intervalElapsed);
    }

    private record SampleDecision(boolean accepted, boolean moved, boolean intervalElapsed) {
    }

    private static final class MutableTrail {
        private final String moveId;
        private final String subjectId;
        private final String dimension;
        private final List<NpcTrailPoint> points = new ArrayList<>();
        private long firstServerTick;
        private long lastServerTick;
        private NpcTrailStatus status;
        private long trailRevision;

        MutableTrail(NpcTrailEvent event, NpcTrailConfig config) {
            moveId = event.moveId();
            subjectId = event.subjectId();
            dimension = event.dimension();
            firstServerTick = event.point().serverTick();
            add(event, config);
        }

        void add(NpcTrailEvent event, NpcTrailConfig config) {
            points.add(event.point());
            while (points.size() > config.maxPointsPerTrail()) {
                points.remove(0);
            }
            firstServerTick = points.get(0).serverTick();
            lastServerTick = event.point().serverTick();
            status = event.status();
            trailRevision = event.trailRevision();
        }

        NpcMovementTrail snapshot() {
            return new NpcMovementTrail(moveId, subjectId, dimension, points, firstServerTick, lastServerTick,
                    status, trailRevision);
        }
    }
}
