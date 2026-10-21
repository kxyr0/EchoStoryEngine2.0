package io.github.kxyr0.echoanimation.core.runtime;

import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationGraphId;
import io.github.kxyr0.echoanimation.api.graph.AnimationOwnerId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationChannelDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationGraphDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationStateDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationTransitionDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.InterruptionPolicy;
import io.github.kxyr0.echoanimation.api.graph.definition.PlaybackMode;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class AnimationSubjectRuntimeTest {
    private static final AnimationChannelId FACE = new AnimationChannelId("face");
    private static final AnimationChannelId BODY = new AnimationChannelId("body");
    private static final AnimationStateId FACE_IDLE = new AnimationStateId("face/idle");
    private static final AnimationStateId SMILE = new AnimationStateId("face/smile");
    private static final AnimationStateId BLINK = new AnimationStateId("face/blink");
    private static final AnimationStateId BODY_IDLE = new AnimationStateId("body/idle");
    private static final AnimationStateId WALK = new AnimationStateId("body/walk");

    @Test
    void twoSubjectsShareGraphWithoutSharingState() {
        AnimationGraphDefinition graph = graph(List.of());
        AnimationSubjectRuntime first = subject("jdh:first", graph);
        AnimationSubjectRuntime second = subject("jdh:second", graph);
        AnimationLease lease = first.acquire(FACE, owner("story")).lease().orElseThrow();
        first.requestState(lease, SMILE);
        Assertions.assertEquals(SMILE, state(first, FACE));
        Assertions.assertEquals(FACE_IDLE, state(second, FACE));
        Assertions.assertEquals(1L, first.snapshot().graphRevision());
        Assertions.assertEquals(1L, second.snapshot().graphRevision());
    }

    @Test
    void oneChannelChangesWithoutResettingAnother() {
        AnimationSubjectRuntime runtime = subject("jdh:actor", graph(List.of()));
        AnimationLease faceLease = runtime.acquire(FACE, owner("story")).lease().orElseThrow();
        AnimationLease bodyLease = runtime.acquire(BODY, owner("story")).lease().orElseThrow();
        runtime.requestState(bodyLease, WALK);
        runtime.tick(3L);
        runtime.requestState(faceLease, SMILE);
        Assertions.assertEquals(SMILE, state(runtime, FACE));
        Assertions.assertEquals(WALK, state(runtime, BODY));
        Assertions.assertEquals(3L, runtime.snapshot().channels().get(BODY).localTick());
    }

    @Test
    void ownershipConflictPreservesCurrentState() {
        AnimationSubjectRuntime runtime = subject("jdh:actor", graph(List.of()));
        AnimationLease first = runtime.acquire(FACE, owner("story")).lease().orElseThrow();
        AnimationLeaseResult conflict = runtime.acquire(FACE, owner("dialogue"));
        AnimationStateChangeResult changed = runtime.requestState(first, SMILE);
        Assertions.assertEquals(AnimationLeaseResult.Status.CONFLICT, conflict.status());
        Assertions.assertEquals(AnimationStateChangeResult.Status.APPLIED, changed.status());
        Assertions.assertEquals(SMILE, state(runtime, FACE));
    }

    @Test
    void releasedLeaseCannotMutateState() {
        AnimationSubjectRuntime runtime = subject("jdh:actor", graph(List.of()));
        AnimationLease lease = runtime.acquire(FACE, owner("story")).lease().orElseThrow();
        runtime.release(lease);
        AnimationStateChangeResult result = runtime.requestState(lease, SMILE);
        Assertions.assertEquals(AnimationStateChangeResult.Status.LEASE_RELEASED, result.status());
        Assertions.assertEquals(FACE_IDLE, state(runtime, FACE));
    }

    @Test
    void holdLastFrameClampsWithoutImplicitIdle() {
        AnimationSubjectRuntime runtime = subject("jdh:actor", graph(List.of()));
        AnimationLease lease = runtime.acquire(FACE, owner("story")).lease().orElseThrow();
        runtime.requestState(lease, SMILE);
        runtime.tick(99L);
        var face = runtime.snapshot().channels().get(FACE);
        Assertions.assertEquals(SMILE, face.stateId());
        Assertions.assertEquals(5L, face.localTick());
        Assertions.assertTrue(face.clipComplete());
    }

    @Test
    void waitForExitRequiresExplicitEvent() {
        AnimationSubjectRuntime runtime = subject("jdh:actor", graph(List.of(
                transition(SMILE, "exit", FACE_IDLE, 0))));
        AnimationLease lease = runtime.acquire(FACE, owner("story")).lease().orElseThrow();
        runtime.requestState(lease, SMILE);
        runtime.tick(8L);
        Assertions.assertEquals(SMILE, state(runtime, FACE));
        Assertions.assertTrue(runtime.snapshot().channels().get(FACE).waitingForExit());
        runtime.signal(lease, "exit");
        Assertions.assertEquals(FACE_IDLE, state(runtime, FACE));
    }

    @Test
    void matchingTransitionUsesAuthoredOrder() {
        AnimationSubjectRuntime runtime = subject("jdh:actor", graph(List.of(
                transition(FACE_IDLE, "mood", SMILE, 10),
                transition(FACE_IDLE, "mood", BLINK, 20))));
        AnimationLease lease = runtime.acquire(FACE, owner("story")).lease().orElseThrow();
        AnimationStateChangeResult result = runtime.signal(lease, "mood");
        Assertions.assertEquals(AnimationStateChangeResult.Status.APPLIED, result.status());
        Assertions.assertEquals(SMILE, state(runtime, FACE));
    }

    @Test
    void duplicateTransitionOrderIsAmbiguous() {
        AnimationSubjectRuntime runtime = subject("jdh:actor", graph(List.of(
                transition(FACE_IDLE, "mood", SMILE, 10),
                transition(FACE_IDLE, "mood", BLINK, 10))));
        AnimationLease lease = runtime.acquire(FACE, owner("story")).lease().orElseThrow();
        AnimationStateChangeResult result = runtime.signal(lease, "mood");
        Assertions.assertEquals(AnimationStateChangeResult.Status.AMBIGUOUS_TRANSITION, result.status());
        Assertions.assertEquals(FACE_IDLE, state(runtime, FACE));
    }

    private static AnimationSubjectRuntime subject(String id, AnimationGraphDefinition graph) {
        return new AnimationSubjectRuntime(new AnimationSubjectId(id), graph);
    }

    private static AnimationOwnerId owner(String path) {
        return new AnimationOwnerId("test:" + path);
    }

    private static AnimationStateId state(AnimationSubjectRuntime runtime, AnimationChannelId channelId) {
        return runtime.snapshot().channels().get(channelId).stateId();
    }

    private static AnimationTransitionDefinition transition(
            AnimationStateId from,
            String eventId,
            AnimationStateId to,
            int order
    ) {
        return new AnimationTransitionDefinition(FACE, from, eventId, to, order);
    }

    private static AnimationGraphDefinition graph(List<AnimationTransitionDefinition> transitions) {
        return new AnimationGraphDefinition(new AnimationGraphId("jdh:face_graph"), 1L,
                List.of(new AnimationChannelDefinition(FACE, FACE_IDLE),
                        new AnimationChannelDefinition(BODY, BODY_IDLE)),
                List.of(state(FACE_IDLE, FACE, PlaybackMode.STOP_AT_END, InterruptionPolicy.IMMEDIATE),
                        state(SMILE, FACE, PlaybackMode.HOLD_LAST_FRAME, InterruptionPolicy.WAIT_FOR_EXIT),
                        state(BLINK, FACE, PlaybackMode.HOLD_LAST_FRAME, InterruptionPolicy.IMMEDIATE),
                        state(BODY_IDLE, BODY, PlaybackMode.STOP_AT_END, InterruptionPolicy.IMMEDIATE),
                        state(WALK, BODY, PlaybackMode.LOOP, InterruptionPolicy.IMMEDIATE)),
                transitions);
    }

    private static AnimationStateDefinition state(
            AnimationStateId id,
            AnimationChannelId channelId,
            PlaybackMode playbackMode,
            InterruptionPolicy interruptionPolicy
    ) {
        return new AnimationStateDefinition(id, channelId, 5L, playbackMode, interruptionPolicy);
    }
}
