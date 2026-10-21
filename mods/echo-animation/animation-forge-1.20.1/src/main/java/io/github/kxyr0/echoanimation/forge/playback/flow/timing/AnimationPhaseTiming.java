package io.github.kxyr0.echoanimation.forge.playback.flow.timing;

import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import java.util.Comparator;

/**
 * Resolves linked phase durations and safe loop exit ticks.
 */
public final class AnimationPhaseTiming {
    public long duration(AnimationDefinition definition, PlaybackRecord playback) {
        return switch (playback.phase) {
            case ENTER -> Math.max(1L, definition.entryVariants().stream()
                    .filter(entry -> entry.clipId().equals(playback.entryClipId))
                    .filter(entry -> entry.segmentId().equals(playback.entrySegmentId)).findFirst().orElseThrow()
                    .durationTicks());
            case MAIN -> definition.main().durationTicks();
            case EXIT -> playback.fallbackExit ? Math.max(1L, definition.blendPolicy().exitTicks())
                    : Math.max(1L, definition.exitVariants().stream()
                            .filter(exit -> playback.selectedExitPoint.filter(exit.exitPointId()::equals).isPresent())
                            .findFirst().orElseThrow().durationTicks());
            case TERMINAL -> 1L;
        };
    }

    public boolean atExitPoint(PlaybackRecord playback, AnimationDefinition definition) {
        long duration = definition.main().durationTicks();
        if (definition.main().exitPoints().isEmpty()) {
            return playback.phaseElapsedTicks > 0L && playback.phaseElapsedTicks % duration == 0L;
        }
        long pointTick = Math.round(firstExit(definition).progress() * duration);
        long local = playback.phaseElapsedTicks % duration;
        return local == pointTick && playback.phaseElapsedTicks > 0L;
    }

    public String firstExitPoint(AnimationDefinition definition) {
        return firstExit(definition).id();
    }

    private AnimationDefinition.ExitPoint firstExit(AnimationDefinition definition) {
        return definition.main().exitPoints().stream()
                .min(Comparator.comparingDouble(AnimationDefinition.ExitPoint::progress)).orElseThrow();
    }
}
