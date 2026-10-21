package io.github.kxyr0.echovoice.core.lane;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import io.github.kxyr0.echovoice.api.playback.VoiceStopReason;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class VoiceForegroundLaneStateMachineTest {
    @Test
    void stopIsConfirmedBeforeNextStartAndOverlapIsZero() {
        VoiceForegroundLaneStateMachine lane = new VoiceForegroundLaneStateMachine();
        VoicePresentationToken first = token(1, "a");
        VoicePresentationToken second = token(2, "b");
        List<String> timeline = new ArrayList<>();

        assertEquals(VoiceLaneAction.Kind.START, lane.requestStart(first).actions().get(0).kind());
        lane.confirmStarted(first);
        timeline.add("A START");
        assertEquals(1, lane.ownedSourceCount());
        assertEquals(VoiceLaneAction.Kind.STOP, lane.requestStart(second).actions().get(0).kind());
        timeline.add("A STOP");
        VoiceLaneDecision confirmed = lane.confirmStopped(first);
        assertEquals(0, lane.ownedSourceCount());
        assertEquals(VoiceLaneAction.Kind.START, confirmed.actions().get(0).kind());
        lane.confirmStarted(second);
        timeline.add("B START");

        assertEquals(List.of("A START", "A STOP", "B START"), timeline);
        assertEquals(1, lane.ownedSourceCount());
    }

    @Test
    void rapidSkipNeverStartsIntermediatePresentation() {
        VoiceForegroundLaneStateMachine lane = new VoiceForegroundLaneStateMachine();
        VoicePresentationToken first = token(1, "a");
        VoicePresentationToken skipped = token(2, "b");
        VoicePresentationToken finalLine = token(3, "c");

        lane.requestStart(first);
        lane.confirmStarted(first);
        lane.requestStart(skipped);
        lane.requestStart(finalLine);
        VoiceLaneDecision afterStop = lane.confirmStopped(first);

        assertEquals(finalLine, afterStop.actions().get(0).token());
        assertEquals(0, afterStop.actions().stream().filter(action -> action.token().equals(skipped)).count());
    }

    @Test
    void staleAndDuplicateStartsAreRejected() {
        VoiceForegroundLaneStateMachine lane = new VoiceForegroundLaneStateMachine();
        VoicePresentationToken current = token(4, "current");
        lane.requestStart(current);

        assertEquals(VoiceLaneDecision.Kind.REJECTED_DUPLICATE, lane.requestStart(current).kind());
        assertEquals(VoiceLaneDecision.Kind.REJECTED_STALE, lane.requestStart(token(3, "old")).kind());
    }

    @Test
    void explicitStopDropsPendingStart() {
        VoiceForegroundLaneStateMachine lane = new VoiceForegroundLaneStateMachine();
        VoicePresentationToken first = token(1, "a");
        lane.requestStart(first);
        lane.confirmStarted(first);
        lane.requestStart(token(2, "b"));

        lane.requestStop(VoiceStopReason.DIALOGUE_CANCELLED);
        assertEquals(VoiceLaneDecision.Kind.ACCEPTED, lane.confirmStopped(first).kind());
    }

    private VoicePresentationToken token(long generation, String stem) {
        return new VoicePresentationToken(new DialogueSessionId("player-one"), (int) generation, generation,
                VoiceAssetKey.of(DialogueId.parse("jdh:first_morning"), stem));
    }
}
