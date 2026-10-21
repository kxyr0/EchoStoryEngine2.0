package io.github.kxyr0.echoanimation.core.sync;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationChannelDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationGraphDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationStateDefinition;
import io.github.kxyr0.echoanimation.api.sync.AnimationChannelSyncState;
import io.github.kxyr0.echoanimation.api.sync.AnimationSubjectSyncSnapshot;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import io.github.kxyr0.echoanimation.core.runtime.AnimationSubjectSnapshot;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Encodes server-owned runtime snapshots into immutable client sync snapshots.
 */
public final class AnimationSubjectSyncEncoding {
    public AnimationSubjectSyncSnapshot encode(
            AnimationGraphDefinition graph,
            AnimationSubjectSnapshot snapshot,
            AnimationSyncRevision revision,
            long authoritativeTick
    ) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(revision, "revision");
        List<AnimationChannelSyncState> channels = snapshot.channels().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().value()))
                .map(entry -> channel(graph, entry.getKey(), entry.getValue()))
                .toList();
        return new AnimationSubjectSyncSnapshot(snapshot.subjectId(), snapshot.graphRevision(),
                authoritativeTick, revision, channels);
    }

    private AnimationChannelSyncState channel(
            AnimationGraphDefinition graph,
            AnimationChannelId channelId,
            AnimationSubjectSnapshot.ChannelView view
    ) {
        AnimationChannelDefinition channel = graph.channel(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Missing channel definition: " + channelId));
        AnimationStateDefinition state = graph.state(view.stateId())
                .orElseThrow(() -> new IllegalArgumentException("Missing state definition: " + view.stateId()));
        return new AnimationChannelSyncState(channelId, view.stateId(), view.clipId(), Optional.empty(),
                Optional.empty(), view.localTick(),
                view.elapsedTicks(), state.clipDurationTicks(), view.normalizedProgress(), view.loopIteration(),
                view.clipComplete(), view.waitingForExit(), state.playbackMode(), state.playbackSpeed(),
                channel.visualOrder(), channel.compositionPolicy(), channel.boneMask(), 1.0D,
                previousClip(graph, view), transitionElapsed(view), transitionDuration(view), transitionProgress(view));
    }

    private Optional<AnimationClipId> previousClip(
            AnimationGraphDefinition graph,
            AnimationSubjectSnapshot.ChannelView view
    ) {
        return view.transition().flatMap(transition -> graph.state(transition.fromStateId()))
                .flatMap(AnimationStateDefinition::clipId);
    }

    private long transitionElapsed(AnimationSubjectSnapshot.ChannelView view) {
        return view.transition().map(AnimationSubjectSnapshot.TransitionView::elapsedTicks).orElse(0L);
    }

    private long transitionDuration(AnimationSubjectSnapshot.ChannelView view) {
        return view.transition().map(AnimationSubjectSnapshot.TransitionView::durationTicks).orElse(0L);
    }

    private double transitionProgress(AnimationSubjectSnapshot.ChannelView view) {
        return view.transition().map(AnimationSubjectSnapshot.TransitionView::blendProgress).orElse(1.0D);
    }
}
