package io.github.kxyr0.echoitemattachments.forge.runtime.source;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.entity.item.ItemEntity;

public record SourceSelection(Status status, Optional<ItemEntity> entity,
        String diagnosticCode, String message) {
    public SourceSelection {
        status = Objects.requireNonNull(status, "status");
        entity = Objects.requireNonNull(entity, "entity");
        diagnosticCode = Objects.requireNonNull(diagnosticCode, "diagnostic code");
        message = Objects.requireNonNull(message, "message");
    }

    public enum Status {
        RESOLVED,
        NOT_FOUND,
        ITEM_MISMATCH,
        COUNT_MISMATCH,
        AMBIGUOUS,
        DIMENSION_UNAVAILABLE
    }
}
