package io.github.kxyr0.echoplayersleep.core.transition;

import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepVisualSnapshot;
import io.github.kxyr0.echoplayersleep.api.visual.PlayerSleepVisualProfileId;
import io.github.kxyr0.echoplayersleep.api.world.Ticks;
import java.util.Objects;

public record PlayerSleepEyeTransition(
        PlayerSleepRevision revision,
        long startedTick,
        Ticks duration,
        boolean closing,
        boolean sleepyBlink) {
    public PlayerSleepEyeTransition {
        revision = Objects.requireNonNull(revision, "revision");
        duration = Objects.requireNonNull(duration, "duration");
    }

    public PlayerSleepVisualSnapshot snapshotAt(long tick, boolean blackFramePublished) {
        double linear = duration.value() == 0L ? 1.0D : (tick - startedTick) / (double) duration.value();
        double bounded = Math.max(0.0D, Math.min(1.0D, linear));
        double eased = bounded * bounded * (3.0D - 2.0D * bounded);
        double coverage = closing ? eased : 1.0D - eased;
        return new PlayerSleepVisualSnapshot(PlayerSleepVisualProfileId.EYELIDS, revision, coverage,
                closing, blackFramePublished, sleepyBlink);
    }
}
