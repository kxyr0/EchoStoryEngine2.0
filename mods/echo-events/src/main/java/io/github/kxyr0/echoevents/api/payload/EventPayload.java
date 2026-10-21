package io.github.kxyr0.echoevents.api.payload;

import io.github.kxyr0.echoevents.api.catalog.EventKind;

public sealed interface EventPayload permits DialogueFact, SceneFact, CutsceneFact,
        ObservationFact, NpcFact, AnimationFact, EventPayload.TeleportFact {
    EventKind kind();

    record TeleportFact(EventKind kind, io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId subject)
            implements EventPayload {
        public TeleportFact {
            java.util.Objects.requireNonNull(subject);
            if (kind != EventKind.TELEPORT_STARTED && kind != EventKind.TELEPORT_COMMITTED
                    && kind != EventKind.TELEPORT_COMPLETED && kind != EventKind.TELEPORT_CANCELLED
                    && kind != EventKind.TELEPORT_FAILED) throw new IllegalArgumentException("Not a teleport event");
        }
    }
}
