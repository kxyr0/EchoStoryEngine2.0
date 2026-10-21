package io.github.kxyr0.echonpc.api.scene;

import io.github.kxyr0.echonpc.api.service.NpcActorView;
import java.util.Objects;
import java.util.Optional;

public record NpcPlaceResult(
        NpcPlaceStatus status,
        Optional<NpcActorView> actor,
        Optional<String> operationId,
        String message
) {
    public NpcPlaceResult {
        status = Objects.requireNonNull(status, "status");
        actor = Objects.requireNonNull(actor, "actor");
        operationId = Objects.requireNonNull(operationId, "operationId");
        message = Objects.requireNonNull(message, "message");
    }

    public NpcPlaceResult(NpcPlaceStatus status, Optional<NpcActorView> actor, String message) {
        this(status, actor, Optional.empty(), message);
    }
}
