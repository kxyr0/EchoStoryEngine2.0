package io.github.kxyr0.echoanimation.api.sync;

import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.List;
import java.util.Objects;

/**
 * Full subject sync snapshot for initial tracking, late join and reconnect.
 */
public record AnimationSubjectSyncSnapshot(
        AnimationSubjectId subjectId,
        long graphRevision,
        long authoritativeTick,
        AnimationSyncRevision syncRevision,
        List<AnimationChannelSyncState> channels,
        List<AnimationPlaybackSyncState> playbacks
) {
    public AnimationSubjectSyncSnapshot(
            AnimationSubjectId subjectId,
            long graphRevision,
            AnimationSyncRevision syncRevision,
            List<AnimationChannelSyncState> channels
    ) {
        this(subjectId, graphRevision, 0L, syncRevision, channels, List.of());
    }

    public AnimationSubjectSyncSnapshot(
            AnimationSubjectId subjectId,
            long graphRevision,
            long authoritativeTick,
            AnimationSyncRevision syncRevision,
            List<AnimationChannelSyncState> channels
    ) {
        this(subjectId, graphRevision, authoritativeTick, syncRevision, channels, List.of());
    }

    public AnimationSubjectSyncSnapshot {
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        syncRevision = Objects.requireNonNull(syncRevision, "syncRevision");
        channels = List.copyOf(Objects.requireNonNull(channels, "channels"));
        playbacks = List.copyOf(Objects.requireNonNull(playbacks, "playbacks"));
        if (graphRevision <= 0L) {
            throw new IllegalArgumentException("graphRevision must be positive.");
        }
        if (authoritativeTick < 0L) {
            throw new IllegalArgumentException("authoritativeTick must not be negative.");
        }
    }
}
