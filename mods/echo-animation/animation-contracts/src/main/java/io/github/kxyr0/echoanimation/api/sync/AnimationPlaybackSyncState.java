package io.github.kxyr0.echoanimation.api.sync;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import java.util.Objects;
import java.util.Optional;

/**
 * High-level lifecycle cursor data synchronized without bone transforms.
 */
public record AnimationPlaybackSyncState(
        AnimationPlaybackId playbackId,
        AnimationId animationId,
        String definitionFingerprint,
        AnimationPhase phase,
        long phaseEnteredTick,
        long phaseElapsedTicks,
        long mainLoopIteration,
        boolean exitRequested,
        Optional<String> selectedExitPoint,
        AnimationStateId targetState,
        long playbackRevision
) {
    public AnimationPlaybackSyncState {
        playbackId = Objects.requireNonNull(playbackId, "playbackId");
        animationId = Objects.requireNonNull(animationId, "animationId");
        definitionFingerprint = Objects.requireNonNull(definitionFingerprint, "definitionFingerprint");
        phase = Objects.requireNonNull(phase, "phase");
        selectedExitPoint = Objects.requireNonNull(selectedExitPoint, "selectedExitPoint");
        targetState = Objects.requireNonNull(targetState, "targetState");
        if (phaseEnteredTick < 0L || phaseElapsedTicks < 0L || mainLoopIteration < 0L || playbackRevision <= 0L) {
            throw new IllegalArgumentException("Playback sync counters are out of range.");
        }
    }
}
