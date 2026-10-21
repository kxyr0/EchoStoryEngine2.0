package io.github.kxyr0.echonpc.api.appearance;

import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import java.util.Objects;
import java.util.Optional;

public record NpcSkinChangeResult(
        NpcSkinChangeStatus status,
        NpcSkinId skinId,
        Optional<NpcActorView> actor,
        Optional<String> operationId,
        String message
) {
    public NpcSkinChangeResult {
        status = Objects.requireNonNull(status, "status");
        skinId = Objects.requireNonNull(skinId, "skinId");
        actor = Objects.requireNonNull(actor, "actor");
        operationId = Objects.requireNonNull(operationId, "operationId");
        message = Objects.requireNonNull(message, "message");
    }

    public NpcSkinChangeResult(NpcSkinChangeStatus status, NpcSkinId skinId,
            Optional<NpcActorView> actor, String message) {
        this(status, skinId, actor, Optional.empty(), message);
    }
}
