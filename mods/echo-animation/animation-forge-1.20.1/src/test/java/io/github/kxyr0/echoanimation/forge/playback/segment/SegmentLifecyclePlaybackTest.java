package io.github.kxyr0.echoanimation.forge.playback.segment;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationBlendPolicy;
import io.github.kxyr0.echoanimation.api.animation.definition.MainMode;
import io.github.kxyr0.echoanimation.api.animation.definition.RecoveryPolicy;
import io.github.kxyr0.echoanimation.api.animation.definition.ReplayPolicy;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeSavedData;
import io.github.kxyr0.echoanimation.forge.playback.flow.AnimationPlaybackStartFlow;
import io.github.kxyr0.echoanimation.forge.playback.flow.AnimationPlaybackTickFlow;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class SegmentLifecyclePlaybackTest {
    private static final AnimationId ANIMATION = AnimationId.parse("jdh:run");
    private static final AnimationSubjectId SUBJECT = new AnimationSubjectId("jdh:actor");
    private static final AnimationChannelId BODY = new AnimationChannelId("body");
    private static final AnimationStateId IDLE = new AnimationStateId("body/idle");

    @Test
    void loopOnlyStartsDirectlyInMainAndCarriesSegmentReference() {
        AnimationRuntimeSavedData data = data();
        start(data, definition(MainMode.LOOP_UNTIL_STOP, 12L));
        var playback = data.activePlaybacks.values().iterator().next();

        Assertions.assertEquals(AnimationPhase.MAIN, playback.phase);
        Assertions.assertEquals("run_loop", playback.mainSegmentId.orElseThrow());
        playback.exitRequested = true;
        AnimationPlaybackTickFlow ticks = new AnimationPlaybackTickFlow();
        for (int tick = 1; tick <= 12; tick++) {
            ticks.tick(data, tick);
        }
        Assertions.assertTrue(data.activePlaybacks.isEmpty());
    }

    @Test
    void oneShotWithoutExitCompletesAfterItsLinkedDuration() {
        AnimationRuntimeSavedData data = data();
        start(data, definition(MainMode.ONCE, 3L));
        AnimationPlaybackTickFlow ticks = new AnimationPlaybackTickFlow();

        ticks.tick(data, 1L);
        ticks.tick(data, 2L);
        ticks.tick(data, 3L);

        Assertions.assertTrue(data.activePlaybacks.isEmpty());
        Assertions.assertEquals(IDLE, data.subjects.get(SUBJECT).channelStates.get(BODY));
    }

    @Test
    void holdLastFrameWaitsForStopThenCompletesWithoutSyntheticExit() {
        AnimationRuntimeSavedData data = data();
        start(data, definition(MainMode.HOLD_UNTIL_STOP, 3L));
        var playback = data.activePlaybacks.values().iterator().next();
        AnimationPlaybackTickFlow ticks = new AnimationPlaybackTickFlow();
        ticks.tick(data, 1L);
        ticks.tick(data, 2L);
        ticks.tick(data, 3L);
        Assertions.assertFalse(data.activePlaybacks.isEmpty());

        playback.exitRequested = true;
        ticks.tick(data, 4L);
        Assertions.assertTrue(data.activePlaybacks.isEmpty());
    }

    @Test
    void fallbackCrossfadeCreatesBoundedFadeWhenExitClipIsAbsent() {
        AnimationRuntimeSavedData data = data();
        AnimationDefinition plain = definition(MainMode.ONCE, 2L);
        AnimationDefinition blended = new AnimationDefinition(plain.id(), plain.controlledChannels(),
                plain.allowedSourceStates(), plain.entryVariants(), plain.main(), plain.exitVariants(),
                plain.markers(), plain.targetState(), plain.recoveryPolicy(), plain.checkpointIntervalTicks(),
                plain.replayPolicy(), new AnimationBlendPolicy(3, 0, 0, Optional.empty()), "fallback");
        start(data, blended);
        AnimationPlaybackTickFlow ticks = new AnimationPlaybackTickFlow();

        ticks.tick(data, 1L);
        ticks.tick(data, 2L);
        Assertions.assertEquals(AnimationPhase.EXIT, data.activePlaybacks.values().iterator().next().phase);
        ticks.tick(data, 3L);
        ticks.tick(data, 4L);
        ticks.tick(data, 5L);
        Assertions.assertTrue(data.activePlaybacks.isEmpty());
    }

    private void start(AnimationRuntimeSavedData data, AnimationDefinition definition) {
        new AnimationPlaybackStartFlow().start(new AnimationPlaybackStartFlow.StartRequest(data, ANIMATION, SUBJECT,
                AnimationCommandId.parse("test:start"), Optional.of(definition), 0L, 1.0D));
    }

    private AnimationRuntimeSavedData data() {
        AnimationRuntimeSavedData data = new AnimationRuntimeSavedData();
        data.ensureSubject(SUBJECT).channelStates.put(BODY, IDLE);
        return data;
    }

    private AnimationDefinition definition(MainMode mode, long duration) {
        AnimationDefinition.MainPhase main = new AnimationDefinition.MainPhase(AnimationClipId.parse("jdh:run"),
                Optional.of("run_loop"), Optional.empty(), mode, duration, List.of());
        return new AnimationDefinition(ANIMATION, Set.of(BODY), Set.of(), List.of(), main, List.of(), List.of(),
                IDLE, RecoveryPolicy.BOUNDED, 3, ReplayPolicy.REJECT_IF_RUNNING,
                AnimationBlendPolicy.NONE, "fingerprint-" + mode);
    }
}
