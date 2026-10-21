package io.github.kxyr0.echoanimation.core.sync;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationGraphId;
import io.github.kxyr0.echoanimation.api.graph.AnimationOwnerId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationChannelDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationGraphDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationStateDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationTransitionDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.ChannelCompositionPolicy;
import io.github.kxyr0.echoanimation.api.graph.definition.InterruptionPolicy;
import io.github.kxyr0.echoanimation.api.graph.definition.PlaybackMode;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.core.runtime.AnimationLease;
import io.github.kxyr0.echoanimation.core.runtime.AnimationSubjectRuntime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class AnimationSubjectSyncTrackerTest {
    private static final AnimationChannelId BODY = new AnimationChannelId("body");
    private static final AnimationStateId IDLE = new AnimationStateId("body/idle");
    private static final AnimationStateId WAVE = new AnimationStateId("body/wave");

    @Test
    void startTrackingSendsFullSnapshotWithCompositionAndPlaybackData() {
        AnimationSubjectRuntime runtime = runtime();
        var dispatch = new AnimationSubjectSyncTracker().startTracking("player-a", graph(), runtime.snapshot(), 42L);
        var channel = dispatch.snapshot().channels().get(0);

        Assertions.assertTrue(dispatch.full());
        Assertions.assertEquals(42L, dispatch.snapshot().authoritativeTick());
        Assertions.assertEquals(1L, dispatch.snapshot().syncRevision().value());
        Assertions.assertEquals(20L, channel.durationTicks());
        Assertions.assertEquals(PlaybackMode.LOOP, channel.playbackMode());
        Assertions.assertEquals(Set.of(BoneId.parse("right_forearm")), channel.boneMask());
        Assertions.assertEquals(10, channel.visualOrder());
    }

    @Test
    void cursorOnlyTicksDoNotPublishButLateJoinReceivesFreshCursor() {
        AnimationSubjectRuntime runtime = runtime();
        AnimationSubjectSyncTracker tracker = new AnimationSubjectSyncTracker();
        tracker.startTracking("player-a", graph(), runtime.snapshot(), 10L);
        runtime.tick(5L);

        Assertions.assertTrue(tracker.publishChange(graph(), runtime.snapshot(), 15L).isEmpty());
        var lateJoin = tracker.startTracking("player-b", graph(), runtime.snapshot(), 15L);

        Assertions.assertEquals(1L, lateJoin.snapshot().syncRevision().value());
        Assertions.assertEquals(5L, lateJoin.snapshot().channels().get(0).elapsedTicks());
        Assertions.assertEquals(15L, lateJoin.snapshot().authoritativeTick());
    }

    @Test
    void transitionPublishesDeltaAndStopTrackingUnsubscribes() {
        AnimationSubjectRuntime runtime = runtime();
        AnimationSubjectSyncTracker tracker = new AnimationSubjectSyncTracker();
        tracker.startTracking("player-a", graph(), runtime.snapshot(), 1L);
        AnimationLease lease = runtime.acquire(BODY, new AnimationOwnerId("test:story")).lease().orElseThrow();

        runtime.signal(lease, "wave");
        var updates = tracker.publishChange(graph(), runtime.snapshot(), 2L);
        var channel = updates.get(0).snapshot().channels().get(0);

        Assertions.assertEquals(1, updates.size());
        Assertions.assertFalse(updates.get(0).full());
        Assertions.assertEquals(2L, updates.get(0).snapshot().syncRevision().value());
        Assertions.assertEquals(Optional.of(AnimationClipId.parse("jdh:idle")), channel.previousClipId());
        Assertions.assertEquals(10L, channel.transitionDurationTicks());

        Assertions.assertTrue(tracker.stopTracking("player-a", runtime.snapshot().subjectId()).isPresent());
        runtime.tick(20L);
        Assertions.assertTrue(tracker.publishChange(graph(), runtime.snapshot(), 22L).isEmpty());
    }

    private AnimationSubjectRuntime runtime() {
        return new AnimationSubjectRuntime(new AnimationSubjectId("jdh:actor"), graph());
    }

    private AnimationGraphDefinition graph() {
        return new AnimationGraphDefinition(new AnimationGraphId("jdh:humanoid_graph"), 1L,
                List.of(new AnimationChannelDefinition(BODY, IDLE, 10, ChannelCompositionPolicy.OVERRIDE,
                        Set.of(BoneId.parse("right_forearm")))),
                List.of(state(IDLE, "jdh:idle", PlaybackMode.LOOP),
                        state(WAVE, "jdh:wave", PlaybackMode.HOLD_LAST_FRAME)),
                List.of(new AnimationTransitionDefinition(BODY, IDLE, "wave", WAVE, 0, 10L)));
    }

    private AnimationStateDefinition state(AnimationStateId id, String clipId, PlaybackMode mode) {
        return new AnimationStateDefinition(id, BODY, 20L, mode, InterruptionPolicy.IMMEDIATE,
                Optional.of(AnimationClipId.parse(clipId)), 1.0D);
    }
}
