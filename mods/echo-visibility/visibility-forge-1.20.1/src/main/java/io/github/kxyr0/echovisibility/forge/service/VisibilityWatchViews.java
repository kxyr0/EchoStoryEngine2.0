package io.github.kxyr0.echovisibility.forge.service;

import io.github.kxyr0.echovisibility.core.arm.VisibilityArmSnapshot;
import io.github.kxyr0.echovisibility.forge.api.VisibilityWatchView;

public final class VisibilityWatchViews {
    private VisibilityWatchViews() {
    }

    public static VisibilityWatchView from(VisibilityArmSnapshot snapshot) {
        return new VisibilityWatchView(snapshot.player(), snapshot.definition(), snapshot.generation(),
                snapshot.ownerLineage(), snapshot.status(), snapshot.lastState(), snapshot.lastTransition(),
                snapshot.lastAcceptedSequence(), snapshot.lastAcceptedServerTick());
    }
}
