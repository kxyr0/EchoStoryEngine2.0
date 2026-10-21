package io.github.kxyr0.echoanimation.core.sync;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationGraphDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.PlaybackMode;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.api.sync.AnimationChannelSyncState;
import io.github.kxyr0.echoanimation.api.sync.AnimationSubjectSyncSnapshot;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import io.github.kxyr0.echoanimation.core.runtime.AnimationSubjectSnapshot;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Server-thread subscription and revision state for animation subject sync.
 */
public final class AnimationSubjectSyncTracker {
    private final AnimationSubjectSyncEncoding encoding = new AnimationSubjectSyncEncoding();
    private final Map<AnimationSubjectId, AnimationSubjectSyncSnapshot> lastSnapshots = new HashMap<>();
    private final Map<AnimationSubjectId, AnimationSyncRevision> revisions = new HashMap<>();
    private final Set<TrackingKey> tracking = new HashSet<>();

    public Dispatch startTracking(
            String subscriberId,
            AnimationGraphDefinition graph,
            AnimationSubjectSnapshot snapshot,
            long authoritativeTick
    ) {
        requireSubscriber(subscriberId);
        SnapshotUpdate update = current(graph, snapshot, authoritativeTick);
        tracking.add(new TrackingKey(subscriberId, snapshot.subjectId()));
        return new Dispatch(subscriberId, update.snapshot(), true);
    }

    public Optional<Removal> stopTracking(String subscriberId, AnimationSubjectId subjectId) {
        requireSubscriber(subscriberId);
        Objects.requireNonNull(subjectId, "subjectId");
        TrackingKey key = new TrackingKey(subscriberId, subjectId);
        if (!tracking.remove(key)) {
            return Optional.empty();
        }
        return Optional.of(new Removal(subscriberId, subjectId, revision(subjectId).next()));
    }

    public List<Dispatch> publishChange(
            AnimationGraphDefinition graph,
            AnimationSubjectSnapshot snapshot,
            long authoritativeTick
    ) {
        SnapshotUpdate update = current(graph, snapshot, authoritativeTick);
        if (!update.changed()) {
            return List.of();
        }
        return tracking.stream()
                .filter(key -> key.subjectId().equals(snapshot.subjectId()))
                .map(key -> new Dispatch(key.subscriberId(), update.snapshot(), update.full()))
                .toList();
    }

    private SnapshotUpdate current(
            AnimationGraphDefinition graph,
            AnimationSubjectSnapshot snapshot,
            long authoritativeTick
    ) {
        AnimationSyncRevision revision = revision(snapshot.subjectId());
        AnimationSubjectSyncSnapshot previous = lastSnapshots.get(snapshot.subjectId());
        AnimationSubjectSyncSnapshot candidate = encoding.encode(graph, snapshot, revision, authoritativeTick);
        boolean changed = previous == null || !sameSyncState(previous, candidate);
        AnimationSyncRevision nextRevision = changed && previous != null ? revision.next() : revision;
        AnimationSubjectSyncSnapshot current = encoding.encode(graph, snapshot, nextRevision, authoritativeTick);
        lastSnapshots.put(snapshot.subjectId(), current);
        revisions.put(snapshot.subjectId(), nextRevision);
        return new SnapshotUpdate(current, changed, previous == null);
    }

    private AnimationSyncRevision revision(AnimationSubjectId subjectId) {
        return revisions.computeIfAbsent(subjectId, ignored -> new AnimationSyncRevision(1L));
    }

    private boolean sameSyncState(AnimationSubjectSyncSnapshot left, AnimationSubjectSyncSnapshot right) {
        return left.graphRevision() == right.graphRevision() && stamps(left).equals(stamps(right));
    }

    private List<ChannelStamp> stamps(AnimationSubjectSyncSnapshot snapshot) {
        return snapshot.channels().stream().map(ChannelStamp::from).toList();
    }

    private void requireSubscriber(String subscriberId) {
        if (Objects.requireNonNull(subscriberId, "subscriberId").isBlank()) {
            throw new IllegalArgumentException("subscriberId must not be blank.");
        }
    }

    public record Dispatch(String subscriberId, AnimationSubjectSyncSnapshot snapshot, boolean full) {
        public Dispatch {
            subscriberId = Objects.requireNonNull(subscriberId, "subscriberId");
            snapshot = Objects.requireNonNull(snapshot, "snapshot");
        }
    }

    public record Removal(
            String subscriberId,
            AnimationSubjectId subjectId,
            AnimationSyncRevision revision
    ) {
        public Removal {
            subscriberId = Objects.requireNonNull(subscriberId, "subscriberId");
            subjectId = Objects.requireNonNull(subjectId, "subjectId");
            revision = Objects.requireNonNull(revision, "revision");
        }
    }

    private record SnapshotUpdate(AnimationSubjectSyncSnapshot snapshot, boolean changed, boolean full) {
    }

    private record TrackingKey(String subscriberId, AnimationSubjectId subjectId) {
    }

    private record ChannelStamp(
            AnimationChannelId channelId,
            Optional<AnimationClipId> clipId,
            PlaybackMode playbackMode,
            boolean clipComplete,
            boolean waitingForExit,
            Optional<AnimationClipId> previousClipId
    ) {
        static ChannelStamp from(AnimationChannelSyncState channel) {
            return new ChannelStamp(channel.channelId(), channel.clipId(), channel.playbackMode(),
                    channel.clipComplete(), channel.waitingForExit(), channel.previousClipId());
        }
    }
}
