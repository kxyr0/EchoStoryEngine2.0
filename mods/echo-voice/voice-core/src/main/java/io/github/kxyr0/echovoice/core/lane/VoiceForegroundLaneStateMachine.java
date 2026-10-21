package io.github.kxyr0.echovoice.core.lane;

import io.github.kxyr0.echovoice.api.playback.VoiceStopReason;
import java.util.Optional;

/** Pure strict stop-before-start state machine for the single foreground lane. */
public final class VoiceForegroundLaneStateMachine {
    private VoicePresentationToken active;
    private VoicePresentationToken starting;
    private VoicePresentationToken pending;
    private long highestGeneration = -1;

    public VoiceLaneDecision requestStart(VoicePresentationToken token) {
        if (token.generation() < highestGeneration) {
            return VoiceLaneDecision.of(VoiceLaneDecision.Kind.REJECTED_STALE);
        }
        if (token.equals(active) || token.equals(starting) || token.equals(pending)) {
            return VoiceLaneDecision.of(VoiceLaneDecision.Kind.REJECTED_DUPLICATE);
        }
        highestGeneration = token.generation();
        if (active != null || starting != null) {
            pending = token;
            VoicePresentationToken old = active != null ? active : starting;
            return VoiceLaneDecision.accepted(VoiceLaneAction.stop(old, VoiceStopReason.LINE_ADVANCED));
        }
        starting = token;
        return VoiceLaneDecision.accepted(VoiceLaneAction.start(token));
    }

    public VoiceLaneDecision requestStop(VoiceStopReason reason) {
        pending = null;
        highestGeneration++;
        VoicePresentationToken current = active != null ? active : starting;
        return current == null
                ? VoiceLaneDecision.of(VoiceLaneDecision.Kind.NO_OP)
                : VoiceLaneDecision.accepted(VoiceLaneAction.stop(current, reason));
    }

    public void confirmStarted(VoicePresentationToken token) {
        if (!token.equals(starting) || active != null) {
            throw new IllegalStateException("Unexpected or overlapping voice start confirmation.");
        }
        active = token;
        starting = null;
    }

    public VoiceLaneDecision confirmStopped(VoicePresentationToken token) {
        if (token.equals(active)) {
            active = null;
        } else if (token.equals(starting)) {
            starting = null;
        } else {
            return VoiceLaneDecision.of(VoiceLaneDecision.Kind.NO_OP);
        }
        if (pending == null) {
            return VoiceLaneDecision.of(VoiceLaneDecision.Kind.ACCEPTED);
        }
        starting = pending;
        pending = null;
        return VoiceLaneDecision.accepted(VoiceLaneAction.start(starting));
    }

    public void complete(VoicePresentationToken token) {
        if (token.equals(active)) {
            active = null;
        }
    }

    public boolean isCurrent(VoicePresentationToken token) {
        return token.generation() == highestGeneration
                && (token.equals(active) || token.equals(starting) || token.equals(pending));
    }

    public int ownedSourceCount() {
        return active == null ? 0 : 1;
    }

    public Optional<VoicePresentationToken> active() {
        return Optional.ofNullable(active);
    }
}
