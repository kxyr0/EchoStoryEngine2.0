package io.github.kxyr0.echoanimation.api.sync;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.graph.definition.ChannelCompositionPolicy;
import io.github.kxyr0.echoanimation.api.graph.definition.PlaybackMode;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable authoritative channel cursor data, not sampled bone transforms.
 */
public record AnimationChannelSyncState(
        AnimationChannelId channelId,
        AnimationStateId stateId,
        Optional<AnimationClipId> clipId,
        Optional<String> segmentId,
        Optional<String> pointId,
        long localTick,
        long elapsedTicks,
        long durationTicks,
        double normalizedProgress,
        long loopIteration,
        boolean clipComplete,
        boolean waitingForExit,
        PlaybackMode playbackMode,
        double playbackSpeed,
        int visualOrder,
        ChannelCompositionPolicy compositionPolicy,
        Set<BoneId> boneMask,
        double weight,
        Optional<AnimationClipId> previousClipId,
        long transitionElapsedTicks,
        long transitionDurationTicks,
        double transitionProgress
) {
    public AnimationChannelSyncState {
        channelId = Objects.requireNonNull(channelId, "channelId");
        stateId = Objects.requireNonNull(stateId, "stateId");
        clipId = Objects.requireNonNull(clipId, "clipId");
        segmentId = Objects.requireNonNull(segmentId, "segmentId");
        pointId = Objects.requireNonNull(pointId, "pointId");
        playbackMode = Objects.requireNonNull(playbackMode, "playbackMode");
        compositionPolicy = Objects.requireNonNull(compositionPolicy, "compositionPolicy");
        boneMask = Set.copyOf(Objects.requireNonNull(boneMask, "boneMask"));
        previousClipId = Objects.requireNonNull(previousClipId, "previousClipId");
        requireTicks(localTick, elapsedTicks, durationTicks, loopIteration);
        requireTransitionTicks(transitionElapsedTicks, transitionDurationTicks);
        requireUnitRange(normalizedProgress, "normalizedProgress");
        requirePositiveFinite(playbackSpeed, "playbackSpeed");
        requireUnitRange(weight, "weight");
        requireUnitRange(transitionProgress, "transitionProgress");
    }

    private static void requireTicks(long localTick, long elapsedTicks, long durationTicks, long loopIteration) {
        if (localTick < 0L || elapsedTicks < 0L || durationTicks <= 0L || loopIteration < 0L) {
            throw new IllegalArgumentException("Channel sync ticks must not be negative.");
        }
    }

    private static void requireTransitionTicks(long elapsedTicks, long durationTicks) {
        if (elapsedTicks < 0L || durationTicks < 0L) {
            throw new IllegalArgumentException("Transition sync ticks must not be negative.");
        }
    }

    private static void requirePositiveFinite(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be positive finite.");
        }
    }

    private static void requireUnitRange(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException(name + " must be finite in range 0..1.");
        }
    }
}
