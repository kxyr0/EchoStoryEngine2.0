package io.github.kxyr0.echoanimation.forge.playback.flow;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackOutcome;
import io.github.kxyr0.echoanimation.api.animation.AnimationStopResult;
import io.github.kxyr0.echoanimation.api.animation.query.AnimationPlaybackQueryResult;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationBlendPolicy;
import io.github.kxyr0.echoanimation.api.animation.definition.MainMode;
import io.github.kxyr0.echoanimation.api.animation.definition.RecoveryPolicy;
import io.github.kxyr0.echoanimation.api.animation.definition.ReplayPolicy;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerDefinition;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerId;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeSavedData;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class AnimationPlaybackFlowTest {
    private static final AnimationId ANIMATION = AnimationId.parse("jdh:knee_bend");
    private static final AnimationId WALK = AnimationId.parse("jdh:humanoid_walk");
    private static final AnimationSubjectId SUBJECT = new AnimationSubjectId("jdh:john");
    private static final AnimationCommandId COMMAND = AnimationCommandId.parse("test:play");
    private static final AnimationChannelId BODY = new AnimationChannelId("body");
    private static final AnimationStateId IDLE = new AnimationStateId("body/idle");

    @Test
    void oncePlaybackRunsEnterMainExitAndWritesTerminalRecord() {
        AnimationRuntimeSavedData data = dataWithSubject();
        AnimationDefinition definition = definition(MainMode.ONCE);
        AnimationPlaybackStartFlow.StartResult started = new AnimationPlaybackStartFlow()
                .start(new AnimationPlaybackStartFlow.StartRequest(data, ANIMATION, SUBJECT, COMMAND,
                        Optional.of(definition), 100L, 1.0D));

        Assertions.assertEquals(AnimationPlayResult.Status.STARTED, started.result().status());
        var playback = data.activePlaybacks.values().iterator().next();
        Assertions.assertEquals(AnimationPhase.ENTER, playback.phase);

        AnimationPlaybackTickFlow tick = new AnimationPlaybackTickFlow();
        Assertions.assertTrue(tick.tick(data, 101L).contains(SUBJECT));
        Assertions.assertEquals(AnimationPhase.MAIN, playback.phase);
        Assertions.assertTrue(tick.tick(data, 102L).isEmpty());
        Assertions.assertTrue(tick.tick(data, 103L).contains(SUBJECT));
        Assertions.assertEquals(AnimationPhase.EXIT, playback.phase);
        Assertions.assertTrue(tick.tick(data, 104L).contains(SUBJECT));

        Assertions.assertTrue(data.activePlaybacks.isEmpty());
        var terminal = data.terminalPlaybacks.values().iterator().next();
        Assertions.assertEquals(AnimationPlaybackOutcome.COMPLETED, terminal.outcome);
        Assertions.assertEquals(IDLE, data.subjects.get(SUBJECT).channelStates.get(BODY));
    }

    @Test
    void explicitStopMovesLongOncePlaybackIntoExitWithoutWaitingForNaturalCompletion() {
        AnimationRuntimeSavedData data = dataWithSubject();
        AnimationDefinition definition = longOnceDefinition();
        new AnimationPlaybackStartFlow().start(new AnimationPlaybackStartFlow.StartRequest(data, ANIMATION,
                SUBJECT, COMMAND, Optional.of(definition), 100L, 1.0D));
        AnimationPlaybackTickFlow tick = new AnimationPlaybackTickFlow();
        tick.tick(data, 101L);
        var playback = data.activePlaybacks.values().iterator().next();
        Assertions.assertEquals(AnimationPhase.MAIN, playback.phase);

        var stopped = new AnimationPlaybackStopFlow().stop(data, SUBJECT, Optional.of(ANIMATION));
        Assertions.assertEquals(AnimationStopResult.Status.STOP_REQUESTED, stopped.result().status());
        Assertions.assertTrue(tick.tick(data, 102L).contains(SUBJECT));
        Assertions.assertEquals(AnimationPhase.EXIT, playback.phase);
        Assertions.assertTrue(tick.tick(data, 103L).contains(SUBJECT));

        Assertions.assertTrue(data.activePlaybacks.isEmpty());
        Assertions.assertEquals(AnimationPlaybackOutcome.GRACEFULLY_STOPPED,
                data.terminalPlaybacks.values().iterator().next().outcome);
        Assertions.assertEquals(IDLE, data.subjects.get(SUBJECT).channelStates.get(BODY));
    }

    @Test
    void occupiedChannelRejectsSecondPlaybackBeforeChangingState() {
        AnimationRuntimeSavedData data = dataWithSubject();
        AnimationDefinition definition = definition(MainMode.LOOP_UNTIL_STOP);
        AnimationPlaybackStartFlow flow = new AnimationPlaybackStartFlow();
        flow.start(new AnimationPlaybackStartFlow.StartRequest(data, ANIMATION, SUBJECT, COMMAND,
                Optional.of(definition), 1L, 1.0D));

        AnimationPlaybackStartFlow.StartResult second = flow.start(new AnimationPlaybackStartFlow.StartRequest(
                data, ANIMATION, SUBJECT, AnimationCommandId.parse("test:second"), Optional.of(definition),
                2L, 1.0D));

        Assertions.assertEquals(AnimationPlayResult.Status.CHANNEL_CONFLICT, second.result().status());
        Assertions.assertEquals(1, data.activePlaybacks.size());
    }

    @Test
    void foregroundBodyPreemptsBackgroundLocomotionButStillKeepsOneBodyPlayback() {
        AnimationRuntimeSavedData data = dataWithSubject();
        AnimationPlaybackStartFlow flow = new AnimationPlaybackStartFlow();
        AnimationDefinition walk = definition(WALK, MainMode.LOOP_UNTIL_STOP);
        AnimationDefinition foreground = definition(ANIMATION, MainMode.ONCE);

        AnimationPlaybackStartFlow.StartResult startedWalk = flow.start(new AnimationPlaybackStartFlow.StartRequest(
                data, WALK, SUBJECT, AnimationCommandId.parse("test:walk"), Optional.of(walk), 1L, 1.0D));
        AnimationPlaybackStartFlow.StartResult startedForeground = flow.start(
                new AnimationPlaybackStartFlow.StartRequest(data, ANIMATION, SUBJECT,
                        AnimationCommandId.parse("test:foreground"), Optional.of(foreground), 2L, 1.0D));

        Assertions.assertEquals(AnimationPlayResult.Status.STARTED, startedWalk.result().status());
        Assertions.assertEquals(AnimationPlayResult.Status.STARTED, startedForeground.result().status());
        Assertions.assertEquals(1, data.activePlaybacks.size());
        Assertions.assertEquals(ANIMATION, data.activePlaybacks.values().iterator().next().animationId);
    }

    @Test
    void serverMarkersDeliverOnceWithPlaybackIdentityAndRevision() {
        AnimationRuntimeSavedData data = dataWithSubject();
        AnimationDefinition base = definition(MainMode.ONCE);
        AnimationMarkerDefinition marker = new AnimationMarkerDefinition(
                AnimationMarkerId.parse("test:take"), AnimationPhase.ENTER, 1L);
        AnimationDefinition definition = new AnimationDefinition(base.id(), base.controlledChannels(),
                base.allowedSourceStates(), base.entryVariants(), base.main(), base.exitVariants(),
                List.of(marker), base.targetState(), base.recoveryPolicy(), base.checkpointIntervalTicks(),
                base.replayPolicy(), base.blendPolicy(), "marker-fingerprint");
        new AnimationPlaybackStartFlow().start(new AnimationPlaybackStartFlow.StartRequest(data, ANIMATION,
                SUBJECT, COMMAND, Optional.of(definition), 100L, 1.0D));
        var delivered = new ArrayList<io.github.kxyr0.echoanimation.forge.api.marker.AnimationMarkerEvent>();

        new AnimationPlaybackTickFlow(delivered::add).tick(data, 101L);

        Assertions.assertEquals(1, delivered.size());
        Assertions.assertEquals(marker.id(), delivered.get(0).markerId());
        Assertions.assertEquals(SUBJECT, delivered.get(0).subjectId());
        Assertions.assertTrue(delivered.get(0).playbackRevision() > 1L);
        Assertions.assertEquals(io.github.kxyr0.echoanimation.forge.api.marker.AnimationMarkerAuthority.SERVER,
                delivered.get(0).authority());
    }

    @Test
    void queryReportsActiveAmbiguousAndTerminalPlaybackByAnimationSubjectPair() {
        AnimationRuntimeSavedData data = dataWithSubject();
        AnimationDefinition definition = definition(MainMode.ONCE);
        AnimationPlaybackStartFlow flow = new AnimationPlaybackStartFlow();
        flow.start(new AnimationPlaybackStartFlow.StartRequest(data, ANIMATION, SUBJECT, COMMAND,
                Optional.of(definition), 1L, 1.0D));

        AnimationPlaybackQueryFlow query = new AnimationPlaybackQueryFlow();
        AnimationPlaybackQueryResult active = query.query(data, List.of(definition), SUBJECT, ANIMATION);
        data.activePlaybacks.put(AnimationPlaybackId.parse("pmanual"), data.activePlaybacks.values().iterator().next());
        AnimationPlaybackQueryResult ambiguous = query.query(data, List.of(definition), SUBJECT, ANIMATION);
        data.activePlaybacks.remove(AnimationPlaybackId.parse("pmanual"));
        AnimationPlaybackTickFlow tick = new AnimationPlaybackTickFlow();
        tick.tick(data, 2L);
        tick.tick(data, 3L);
        tick.tick(data, 4L);
        tick.tick(data, 5L);
        AnimationPlaybackQueryResult terminal = query.query(data, List.of(definition), SUBJECT, ANIMATION);

        Assertions.assertEquals(AnimationPlaybackQueryResult.Status.ACTIVE, active.status());
        Assertions.assertEquals(AnimationPhase.ENTER, active.phase().orElseThrow());
        Assertions.assertEquals(AnimationPlaybackQueryResult.Status.AMBIGUOUS_PLAYBACK, ambiguous.status());
        Assertions.assertEquals(AnimationPlaybackQueryResult.Status.TERMINAL, terminal.status());
        Assertions.assertEquals(AnimationPlaybackOutcome.COMPLETED, terminal.outcome().orElseThrow());
    }

    private AnimationRuntimeSavedData dataWithSubject() {
        AnimationRuntimeSavedData data = new AnimationRuntimeSavedData();
        data.ensureSubject(SUBJECT).channelStates.put(BODY, IDLE);
        return data;
    }

    private AnimationDefinition definition(MainMode mode) {
        return definition(ANIMATION, mode);
    }

    private AnimationDefinition definition(AnimationId animationId, MainMode mode) {
        return new AnimationDefinition(animationId, Set.of(BODY), Set.of(IDLE),
                List.of(new AnimationDefinition.EntryVariant(IDLE, AnimationClipId.parse("jdh:enter"), 1L)),
                new AnimationDefinition.MainPhase(AnimationClipId.parse("jdh:main"), mode, 2L,
                        List.of(new AnimationDefinition.ExitPoint("complete", 1.0D))),
                List.of(new AnimationDefinition.ExitVariant("complete", AnimationClipId.parse("jdh:exit"), 1L)),
                List.of(), IDLE, RecoveryPolicy.BOUNDED, 1, ReplayPolicy.REJECT_IF_RUNNING,
                AnimationBlendPolicy.NONE, "fingerprint");
    }

    private AnimationDefinition longOnceDefinition() {
        AnimationDefinition base = definition(MainMode.ONCE);
        return new AnimationDefinition(base.id(), base.controlledChannels(), base.allowedSourceStates(),
                base.entryVariants(), new AnimationDefinition.MainPhase(base.main().clipId(), MainMode.ONCE,
                        10_010L, base.main().exitPoints()), base.exitVariants(), base.markers(), base.targetState(),
                base.recoveryPolicy(), base.checkpointIntervalTicks(), base.replayPolicy(), base.blendPolicy(),
                "long-once-fingerprint");
    }
}
