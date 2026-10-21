package io.github.kxyr0.echoanimation.api.animation.definition;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerDefinition;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * High-level animation lifecycle definition.
 *
 * Supports both direct MAIN playback (no authored Entry/Exit clips) and the
 * full ENTER -> MAIN -> EXIT lifecycle. MainMode controls one-shot, loop and
 * hold-until-stop behavior without fake long-duration clips.
 */
public record AnimationDefinition(
        AnimationId id,
        Set<AnimationChannelId> controlledChannels,
        Set<AnimationStateId> allowedSourceStates,
        List<EntryVariant> entryVariants,
        MainPhase main,
        List<ExitVariant> exitVariants,
        List<AnimationMarkerDefinition> markers,
        AnimationStateId targetState,
        RecoveryPolicy recoveryPolicy,
        int checkpointIntervalTicks,
        ReplayPolicy replayPolicy,
        AnimationBlendPolicy blendPolicy,
        String fingerprint
) {
    public AnimationDefinition {
        id = Objects.requireNonNull(id, "id");
        controlledChannels = Set.copyOf(Objects.requireNonNull(controlledChannels, "controlledChannels"));
        allowedSourceStates = Set.copyOf(Objects.requireNonNull(allowedSourceStates, "allowedSourceStates"));
        entryVariants = List.copyOf(Objects.requireNonNull(entryVariants, "entryVariants"));
        main = Objects.requireNonNull(main, "main");
        exitVariants = List.copyOf(Objects.requireNonNull(exitVariants, "exitVariants"));
        markers = List.copyOf(Objects.requireNonNull(markers, "markers"));
        targetState = Objects.requireNonNull(targetState, "targetState");
        recoveryPolicy = Objects.requireNonNull(recoveryPolicy, "recoveryPolicy");
        replayPolicy = Objects.requireNonNull(replayPolicy, "replayPolicy");
        blendPolicy = Objects.requireNonNull(blendPolicy, "blendPolicy");
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        if (controlledChannels.isEmpty()) {
            throw new IllegalArgumentException("AnimationDefinition requires at least one channel.");
        }
        if (checkpointIntervalTicks < 0) {
            throw new IllegalArgumentException("checkpointIntervalTicks must not be negative.");
        }
        if (new HashSet<>(markers.stream().map(AnimationMarkerDefinition::id).toList()).size() != markers.size()) {
            throw new IllegalArgumentException("Animation marker ids must be unique.");
        }
    }

    public record EntryVariant(
            AnimationStateId sourceState,
            AnimationClipId clipId,
            Optional<String> segmentId,
            long durationTicks
    ) {
        public EntryVariant(AnimationStateId sourceState, AnimationClipId clipId, long durationTicks) {
            this(sourceState, clipId, Optional.empty(), durationTicks);
        }

        public EntryVariant {
            sourceState = Objects.requireNonNull(sourceState, "sourceState");
            clipId = Objects.requireNonNull(clipId, "clipId");
            segmentId = Objects.requireNonNull(segmentId, "segmentId");
            if (durationTicks < 0L) {
                throw new IllegalArgumentException("durationTicks must not be negative.");
            }
        }
    }

    public record MainPhase(
            AnimationClipId clipId,
            Optional<String> segmentId,
            Optional<String> pointId,
            MainMode mode,
            long durationTicks,
            List<ExitPoint> exitPoints
    ) {
        public MainPhase(AnimationClipId clipId, MainMode mode, long durationTicks, List<ExitPoint> exitPoints) {
            this(clipId, Optional.empty(), Optional.empty(), mode, durationTicks, exitPoints);
        }

        public MainPhase {
            clipId = Objects.requireNonNull(clipId, "clipId");
            segmentId = Objects.requireNonNull(segmentId, "segmentId");
            pointId = Objects.requireNonNull(pointId, "pointId");
            mode = Objects.requireNonNull(mode, "mode");
            exitPoints = List.copyOf(Objects.requireNonNull(exitPoints, "exitPoints"));
            if (durationTicks < 0L) {
                throw new IllegalArgumentException("durationTicks must not be negative.");
            }
        }
    }

    public record ExitPoint(String id, double progress, Optional<String> sourcePointId) {
        public ExitPoint(String id, double progress) {
            this(id, progress, Optional.empty());
        }

        public ExitPoint {
            id = Objects.requireNonNull(id, "id").trim();
            sourcePointId = Objects.requireNonNull(sourcePointId, "sourcePointId");
            if (!id.matches("[a-z0-9_./-]+") || id.isEmpty()) {
                throw new IllegalArgumentException("Exit point id contains invalid characters.");
            }
            if (!Double.isFinite(progress) || progress < 0.0D || progress > 1.0D) {
                throw new IllegalArgumentException("Exit point progress must be finite in range 0..1.");
            }
        }
    }

    public record ExitVariant(
            String exitPointId,
            AnimationClipId clipId,
            Optional<String> segmentId,
            long durationTicks
    ) {
        public ExitVariant(String exitPointId, AnimationClipId clipId, long durationTicks) {
            this(exitPointId, clipId, Optional.empty(), durationTicks);
        }

        public ExitVariant {
            exitPointId = Objects.requireNonNull(exitPointId, "exitPointId").trim();
            clipId = Objects.requireNonNull(clipId, "clipId");
            segmentId = Objects.requireNonNull(segmentId, "segmentId");
            if (durationTicks < 0L) {
                throw new IllegalArgumentException("durationTicks must not be negative.");
            }
        }
    }
}
