package io.github.kxyr0.echonpc.api.rest;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.util.Objects;
import java.util.Optional;

/**
 * Neutral rest lifecycle event for adapters and diagnostics.
 */
public record NpcRestLifecycleEvent(
        ActorId npcId,
        Kind kind,
        Optional<NpcRestAttachmentView> attachment,
        String diagnosticCode
) {
    public NpcRestLifecycleEvent {
        npcId = Objects.requireNonNull(npcId, "npcId");
        kind = Objects.requireNonNull(kind, "kind");
        attachment = Objects.requireNonNull(attachment, "attachment");
        diagnosticCode = Objects.requireNonNull(diagnosticCode, "diagnosticCode");
    }

    public enum Kind {
        ATTACHED,
        WOKE_UP,
        RECOVERED,
        FAILED
    }
}
