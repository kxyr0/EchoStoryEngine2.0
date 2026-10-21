package io.github.kxyr0.echonpcbackpack.api.view;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.payload.NpcBackpackPayloadRef;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackAttachmentPoint;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackStatus;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionSnapshot;
import java.util.Objects;
import java.util.Optional;

public record NpcBackpackView(
        ActorId actorId,
        NpcBackpackStatus status,
        NpcBackpackAttachmentPoint attachmentPoint,
        Optional<NpcBackpackPayloadRef> payload,
        Optional<NpcBackpackTransitionSnapshot> transition,
        Optional<NpcBackpackDiagnostic> diagnostic,
        long revision
) {
    public NpcBackpackView {
        actorId = Objects.requireNonNull(actorId, "actorId");
        status = Objects.requireNonNull(status, "status");
        attachmentPoint = Objects.requireNonNull(attachmentPoint, "attachmentPoint");
        payload = Objects.requireNonNull(payload, "payload");
        transition = Objects.requireNonNull(transition, "transition");
        diagnostic = Objects.requireNonNull(diagnostic, "diagnostic");
        if (revision < 1L) {
            throw new IllegalArgumentException("revision must be positive.");
        }
    }
}
