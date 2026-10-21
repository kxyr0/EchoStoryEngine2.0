package io.github.kxyr0.echonavigation.forge.api.view;

import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.List;
import java.util.Objects;

public record NavigationPathView(
        NavigationMoveId moveId,
        NavigationBackendId backendId,
        Vector3 currentPosition,
        NavigationDestination destination,
        List<Vector3> pathNodes,
        int currentNodeIndex,
        NavigationStatus status,
        String stuckState) {
    public NavigationPathView {
        moveId = Objects.requireNonNull(moveId, "moveId");
        backendId = Objects.requireNonNull(backendId, "backendId");
        currentPosition = Objects.requireNonNull(currentPosition, "currentPosition");
        destination = Objects.requireNonNull(destination, "destination");
        pathNodes = List.copyOf(Objects.requireNonNull(pathNodes, "pathNodes"));
        status = Objects.requireNonNull(status, "status");
        stuckState = Objects.requireNonNull(stuckState, "stuckState");
    }
}
