package io.github.kxyr0.echoanimation.core.playback;

import io.github.kxyr0.echoanimation.api.graph.definition.AnimationStateDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.PlaybackMode;
import java.util.Objects;

/**
 * Server-authoritative deterministic cursor derived from logical ticks.
 */
public record PlaybackCursor(
        long sampledTick,
        long elapsedTicks,
        long durationTicks,
        double normalizedProgress,
        long loopIteration,
        boolean clipComplete,
        boolean waitingForExit
) {
    public PlaybackCursor {
        if (sampledTick < 0L || elapsedTicks < 0L || durationTicks <= 0L || loopIteration < 0L) {
            throw new IllegalArgumentException("Playback cursor ticks are out of range.");
        }
        if (!Double.isFinite(normalizedProgress) || normalizedProgress < 0.0D || normalizedProgress > 1.0D) {
            throw new IllegalArgumentException("normalizedProgress must be finite in range 0..1.");
        }
    }

    public static PlaybackCursor from(AnimationStateDefinition state, long elapsedTicks) {
        Objects.requireNonNull(state, "state");
        if (elapsedTicks < 0L) {
            throw new IllegalArgumentException("elapsedTicks must not be negative.");
        }
        long scaled = (long) Math.floor(elapsedTicks * state.playbackSpeed());
        long duration = state.clipDurationTicks();
        if (state.playbackMode() == PlaybackMode.LOOP) {
            return loop(state, elapsedTicks, scaled, duration);
        }
        long sampled = Math.min(scaled, duration);
        return new PlaybackCursor(sampled, elapsedTicks, duration, progress(sampled, duration),
                0L, scaled >= duration, scaled >= duration && state.waitsForExit());
    }

    private static PlaybackCursor loop(
            AnimationStateDefinition state,
            long elapsedTicks,
            long scaled,
            long duration
    ) {
        long sampled = scaled % duration;
        long loops = scaled / duration;
        return new PlaybackCursor(sampled, elapsedTicks, duration, progress(sampled, duration),
                loops, false, state.waitsForExit() && scaled >= duration);
    }

    private static double progress(long sampled, long duration) {
        return Math.min(1.0D, sampled / (double) duration);
    }
}
