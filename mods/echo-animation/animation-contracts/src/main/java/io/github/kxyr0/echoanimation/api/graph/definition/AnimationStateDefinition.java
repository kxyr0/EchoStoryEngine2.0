package io.github.kxyr0.echoanimation.api.graph.definition;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable per-channel state declaration.
 */
public record AnimationStateDefinition(
        AnimationStateId id,
        AnimationChannelId channelId,
        long clipDurationTicks,
        PlaybackMode playbackMode,
        InterruptionPolicy interruptionPolicy,
        Optional<AnimationClipId> clipId,
        double playbackSpeed
) {
    public AnimationStateDefinition(
            AnimationStateId id,
            AnimationChannelId channelId,
            long clipDurationTicks,
            PlaybackMode playbackMode,
            InterruptionPolicy interruptionPolicy
    ) {
        this(id, channelId, clipDurationTicks, playbackMode, interruptionPolicy, Optional.empty(), 1.0D);
    }

    public AnimationStateDefinition {
        id = Objects.requireNonNull(id, "id");
        channelId = Objects.requireNonNull(channelId, "channelId");
        playbackMode = Objects.requireNonNull(playbackMode, "playbackMode");
        interruptionPolicy = Objects.requireNonNull(interruptionPolicy, "interruptionPolicy");
        clipId = Objects.requireNonNull(clipId, "clipId");
        if (clipDurationTicks <= 0L) {
            throw new IllegalArgumentException("clipDurationTicks must be positive.");
        }
        if (!Double.isFinite(playbackSpeed) || playbackSpeed <= 0.0D) {
            throw new IllegalArgumentException("playbackSpeed must be positive finite.");
        }
    }

    public boolean waitsForExit() {
        return interruptionPolicy == InterruptionPolicy.WAIT_FOR_EXIT;
    }
}
