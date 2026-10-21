package io.github.kxyr0.echonpcbackpack.core.transition;

import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackAttachmentPoint;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPhase;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionType;
import java.util.List;

/**
 * Pure transition and semantic marker ordering rules.
 */
public final class BackpackStateMachine {
    public BackpackTransitionDecision advance(BackpackTransitionState state,
            NpcBackpackTransitionPhase expected, NpcBackpackTransitionPhase next) {
        if (state.phase.terminal()) {
            return decision(BackpackTransitionDecision.Status.TERMINAL, state, "ENPC-SB-TERMINAL");
        }
        if (state.phase != expected) {
            return decision(BackpackTransitionDecision.Status.OUT_OF_ORDER, state, "ENPC-SB-PHASE-ORDER");
        }
        state.phase = next;
        state.revision++;
        return decision(BackpackTransitionDecision.Status.ADVANCED, state, "ENPC-SB-ADVANCED");
    }

    public BackpackTransitionDecision marker(BackpackTransitionState state, String marker, String occurrenceId) {
        String receipt = marker + "@" + occurrenceId;
        if (state.markerReceipts.contains(receipt)) {
            return decision(BackpackTransitionDecision.Status.DUPLICATE, state, "ENPC-SB-MARKER-DUPLICATE");
        }
        List<String> expected = BackpackMarkerSequence.required(state.type);
        int completed = completedRequired(state, expected);
        String next = completed < expected.size() ? expected.get(completed) : "";
        if (BackpackMarkerSequence.hand(marker)) {
            BackpackTransitionDecision decision = hand(state, marker, completed);
            if (decision.status() == BackpackTransitionDecision.Status.ADVANCED) {
                state.markerReceipts.add(receipt);
            }
            return decision;
        }
        if (!marker.equals(next)) {
            return decision(BackpackTransitionDecision.Status.OUT_OF_ORDER, state, "ENPC-SB-MARKER-OUT-OF-ORDER");
        }
        state.markerReceipts.add(receipt);
        applyRequired(state, marker);
        state.revision++;
        return decision(BackpackTransitionDecision.Status.ADVANCED, state, "ENPC-SB-MARKER-ADVANCED");
    }

    private BackpackTransitionDecision hand(BackpackTransitionState state, String marker, int completed) {
        boolean valid = state.type == NpcBackpackTransitionType.EQUIP && completed == 1
                || state.type == NpcBackpackTransitionType.UNEQUIP && completed == 1;
        if (!valid) {
            return decision(BackpackTransitionDecision.Status.OUT_OF_ORDER, state, "ENPC-SB-MARKER-OUT-OF-ORDER");
        }
        state.attachmentPoint = marker.equals(BackpackMarkerSequence.LEFT_HAND)
                ? NpcBackpackAttachmentPoint.LEFT_HAND : NpcBackpackAttachmentPoint.RIGHT_HAND;
        state.phase = NpcBackpackTransitionPhase.HELD_IN_HAND;
        state.revision++;
        return decision(BackpackTransitionDecision.Status.ADVANCED, state, "ENPC-SB-MARKER-ADVANCED");
    }

    private int completedRequired(BackpackTransitionState state, List<String> expected) {
        int count = 0;
        for (String marker : expected) {
            if (state.markerReceipts.stream().anyMatch(receipt -> receipt.startsWith(marker + "@"))) count++;
            else break;
        }
        return count;
    }

    private void applyRequired(BackpackTransitionState state, String marker) {
        if (marker.equals(BackpackMarkerSequence.TAKE)) {
            state.phase = NpcBackpackTransitionPhase.SOURCE_REMOVED;
        } else if (marker.equals(BackpackMarkerSequence.BACK)) {
            state.phase = NpcBackpackTransitionPhase.ATTACHED_TO_BACK;
            state.attachmentPoint = NpcBackpackAttachmentPoint.BACK;
        } else if (marker.equals(BackpackMarkerSequence.DETACH)) {
            state.phase = NpcBackpackTransitionPhase.READY_TO_PLACE;
            state.attachmentPoint = NpcBackpackAttachmentPoint.RIGHT_HAND;
        } else if (marker.equals(BackpackMarkerSequence.PLACE)) {
            state.phase = NpcBackpackTransitionPhase.DESTINATION_PLACED;
            state.attachmentPoint = NpcBackpackAttachmentPoint.NONE;
        }
    }

    private BackpackTransitionDecision decision(BackpackTransitionDecision.Status status,
            BackpackTransitionState state, String code) {
        return new BackpackTransitionDecision(status, state.phase, code);
    }
}
