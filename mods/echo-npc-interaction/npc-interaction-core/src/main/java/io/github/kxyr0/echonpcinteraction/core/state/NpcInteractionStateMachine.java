package io.github.kxyr0.echonpcinteraction.core.state;

import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionStatus;
import java.util.EnumMap;
import java.util.Set;

public final class NpcInteractionStateMachine {
    private final EnumMap<NpcInteractionStatus, Set<NpcInteractionStatus>> transitions =
            new EnumMap<>(NpcInteractionStatus.class);

    public NpcInteractionStateMachine() {
        allow(NpcInteractionStatus.CREATED, NpcInteractionStatus.WAITING_FOR_MATERIALIZATION);
        allow(NpcInteractionStatus.WAITING_FOR_MATERIALIZATION, NpcInteractionStatus.VALIDATING_TARGET);
        allow(NpcInteractionStatus.VALIDATING_TARGET, NpcInteractionStatus.SELECTING_PROVIDER);
        allow(NpcInteractionStatus.SELECTING_PROVIDER, NpcInteractionStatus.RESOLVING_APPROACH);
        allow(NpcInteractionStatus.RESOLVING_APPROACH, NpcInteractionStatus.WAITING_FOR_NAVIGATION);
        allow(NpcInteractionStatus.WAITING_FOR_NAVIGATION, NpcInteractionStatus.ALIGNING);
        allow(NpcInteractionStatus.ALIGNING, NpcInteractionStatus.INTERACTING);
        allow(NpcInteractionStatus.INTERACTING, NpcInteractionStatus.VERIFYING);
        allow(NpcInteractionStatus.VERIFYING, NpcInteractionStatus.COMPLETED);
    }

    public boolean canMove(NpcInteractionStatus from, NpcInteractionStatus to) {
        if (from.terminal()) {
            return false;
        }
        return to == NpcInteractionStatus.CANCELLED || to == NpcInteractionStatus.FAILED
                || to == NpcInteractionStatus.UNCERTAIN
                || transitions.getOrDefault(from, Set.of()).contains(to);
    }

    private void allow(NpcInteractionStatus from, NpcInteractionStatus to) {
        transitions.put(from, Set.of(to));
    }
}
