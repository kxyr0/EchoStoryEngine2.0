package io.github.kxyr0.echoanimation.core.runtime;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable read model for one subject runtime.
 */
public record AnimationSubjectSnapshot(
        AnimationSubjectId subjectId,
        long graphRevision,
        Map<AnimationChannelId, ChannelView> channels
) {
    public AnimationSubjectSnapshot {
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        channels = Map.copyOf(Objects.requireNonNull(channels, "channels"));
    }

    public record ChannelView(
            AnimationStateId stateId,
            Optional<AnimationClipId> clipId,
            long localTick,
            long elapsedTicks,
            double normalizedProgress,
            long loopIteration,
            boolean clipComplete,
            boolean waitingForExit,
            Optional<TransitionView> transition
    ) {
        public ChannelView(
                AnimationStateId stateId,
                long localTick,
                boolean clipComplete,
                boolean waitingForExit
        ) {
            this(stateId, Optional.empty(), localTick, localTick, 0.0D, 0L, clipComplete,
                    waitingForExit, Optional.empty());
        }

        public ChannelView {
            stateId = Objects.requireNonNull(stateId, "stateId");
            clipId = Objects.requireNonNull(clipId, "clipId");
            transition = Objects.requireNonNull(transition, "transition");
            if (localTick < 0L || elapsedTicks < 0L || loopIteration < 0L) {
                throw new IllegalArgumentException("ChannelView ticks must not be negative.");
            }
            if (!Double.isFinite(normalizedProgress)
                    || normalizedProgress < 0.0D
                    || normalizedProgress > 1.0D) {
                throw new IllegalArgumentException("normalizedProgress must be finite in range 0..1.");
            }
        }
    }

    public record TransitionView(
            AnimationStateId fromStateId,
            AnimationStateId toStateId,
            long elapsedTicks,
            long durationTicks,
            double blendProgress
    ) {
        public TransitionView {
            fromStateId = Objects.requireNonNull(fromStateId, "fromStateId");
            toStateId = Objects.requireNonNull(toStateId, "toStateId");
            if (elapsedTicks < 0L || durationTicks <= 0L) {
                throw new IllegalArgumentException("Transition ticks are out of range.");
            }
            if (!Double.isFinite(blendProgress) || blendProgress < 0.0D || blendProgress > 1.0D) {
                throw new IllegalArgumentException("blendProgress must be finite in range 0..1.");
            }
        }
    }
}
