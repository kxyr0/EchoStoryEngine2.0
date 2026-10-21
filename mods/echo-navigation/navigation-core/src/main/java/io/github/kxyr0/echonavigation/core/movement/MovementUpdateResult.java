package io.github.kxyr0.echonavigation.core.movement;

import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import java.util.Objects;

public record MovementUpdateResult(NavigationSnapshot snapshot, boolean terminalChanged) {
    public MovementUpdateResult {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }
}
