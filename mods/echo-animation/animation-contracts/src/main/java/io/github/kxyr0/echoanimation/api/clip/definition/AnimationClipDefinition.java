package io.github.kxyr0.echoanimation.api.clip.definition;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.model.AnimationLoopSeam;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable normalized visual clip; graph state owns playback duration. */
public record AnimationClipDefinition( AnimationClipId id, Optional<AnimationRigId> rigId,
        List<AnimationBoneTrackDefinition> tracks, int durationTicks, List<AnimationClipSegment> segments,
        List<AnimationClipPoint> points, List<AnimationClipEvent> events, AnimationRootMotion rootMotion,
        int schemaVersion, AnimationLoopSeam loopSeam, double authoredSpeedBlocksPerSecond,
        String semanticFingerprint) {
    public record LegacySettings( int schemaVersion, AnimationLoopSeam loopSeam, double authoredSpeedBlocksPerSecond,
            String semanticFingerprint) {}

    public static AnimationClipDefinition legacy( AnimationClipId id, Optional<AnimationRigId> rigId,
            List<AnimationBoneTrackDefinition> tracks, LegacySettings settings) {
        return new AnimationClipDefinition( id, rigId, tracks, 0, List.of(), List.of(), List.of(),
                AnimationRootMotion.disabled(), settings.schemaVersion(), settings.loopSeam(),
                settings.authoredSpeedBlocksPerSecond(), settings.semanticFingerprint());
    }

    public AnimationClipDefinition( AnimationClipId id, Optional<AnimationRigId> rigId,
            List<AnimationBoneTrackDefinition> tracks, String semanticFingerprint) {
        this( id, rigId, tracks, 0, List.of(), List.of(), List.of(), AnimationRootMotion.disabled(), 1,
                AnimationLoopSeam.HOLD, 1.0D, semanticFingerprint);
    }

    public AnimationClipDefinition {
        id = Objects.requireNonNull(id, "id");
        rigId = Objects.requireNonNull(rigId, "rigId");
        tracks = List.copyOf(Objects.requireNonNull(tracks, "tracks"));
        segments = List.copyOf(Objects.requireNonNull(segments, "segments"));
        points = List.copyOf(Objects.requireNonNull(points, "points"));
        events = List.copyOf(Objects.requireNonNull(events, "events"));
        rootMotion = Objects.requireNonNull(rootMotion, "rootMotion");
        loopSeam = Objects.requireNonNull(loopSeam, "loopSeam");
        semanticFingerprint = Objects.requireNonNull(semanticFingerprint, "semanticFingerprint");
        validateTiming(schemaVersion, durationTicks, authoredSpeedBlocksPerSecond);
        validateSegments(segments, durationTicks);
        validatePoints(points, segments, durationTicks);
        validateEvents(events, durationTicks);
    }

    private static void validateTiming(int schemaVersion, int durationTicks, double speed) {
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be positive.");
        }
        if (durationTicks < 0) {
            throw new IllegalArgumentException("durationTicks must not be negative.");
        }
        if (!Double.isFinite(speed) || speed <= 0.0D) {
            throw new IllegalArgumentException( "authoredSpeedBlocksPerSecond must be finite and positive.");
        }
    }

    private static void validateSegments(List<AnimationClipSegment> segments, int durationTicks) {
        if (new HashSet<>(segments.stream().map(AnimationClipSegment::id).toList()).size()
                != segments.size()) {
            throw new IllegalArgumentException("Clip segment ids must be unique.");
        }
        for (var segment : segments) {
            if (durationTicks <= 0 || segment.endTick() > durationTicks) {
                throw new IllegalArgumentException( "Segment '" + segment.id() + "' exceeds clip duration.");
            }
        }
    }

    private static void validatePoints( List<AnimationClipPoint> points, List<AnimationClipSegment> segments,
            int durationTicks) {
        if (new HashSet<>(points.stream().map(AnimationClipPoint::id).toList()).size()
                != points.size()) {
            throw new IllegalArgumentException("Clip point ids must be unique.");
        }
        for (var point : points) {
            if (durationTicks <= 0 || point.tick() > durationTicks) {
                throw new IllegalArgumentException( "Point '" + point.id() + "' exceeds clip duration.");
            }
            point.segmentId().ifPresent(owner -> validatePointOwner(point, owner, segments));
        }
    }

    private static void validatePointOwner(
            AnimationClipPoint point, String owner, List<AnimationClipSegment> segments) {
        var segment = segments.stream() .filter(item -> item.id().equals(owner)) .findFirst() .orElseThrow( () ->
                                        new IllegalArgumentException( "Point '" + point.id()
                                                        + "' references unknown segment '" + owner + "'."));
        if (point.tick() < segment.startTick() || point.tick() > segment.endTick()) {
            throw new IllegalArgumentException( "Point '" + point.id() + "' is outside segment '" + owner + "'.");
        }
    }

    private static void validateEvents(List<AnimationClipEvent> events, int durationTicks) {
        if (new HashSet<>(events.stream().map(event -> event.id() + "@" + event.tick()).toList()) .size()
                != events.size()) {
            throw new IllegalArgumentException("Clip events must be unique at the same tick.");
        }
        for (var event : events) {
            if (durationTicks <= 0 || event.tick() > durationTicks) {
                throw new IllegalArgumentException( "Event '" + event.id() + "' exceeds clip duration.");
            }
        }
    }

    public Optional<AnimationClipSegment> segment(String segmentId) {
        return segments.stream().filter(segment -> segment.id().equals(segmentId)).findFirst();
    }

    public Optional<AnimationClipPoint> point(String pointId) {
        return points.stream().filter(point -> point.id().equals(pointId)).findFirst();
    }
}
