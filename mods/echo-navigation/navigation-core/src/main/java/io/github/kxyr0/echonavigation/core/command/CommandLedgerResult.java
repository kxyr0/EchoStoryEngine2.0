package io.github.kxyr0.echonavigation.core.command;

import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import java.util.Objects;
import java.util.Optional;

public record CommandLedgerResult(Status status, Optional<NavigationMoveId> moveId) {
    public CommandLedgerResult {
        status = Objects.requireNonNull(status, "status");
        moveId = Objects.requireNonNull(moveId, "moveId");
    }

    public enum Status {
        RECORDED,
        DUPLICATE,
        CONFLICT
    }
}
