package io.github.kxyr0.echonavigation.api.route;

import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import java.util.List;
import java.util.Objects;

public record NavigationRoute(
        NavigationMoveId moveId,
        NavigationBackendId backendId,
        NavigationSubjectId subjectId,
        String dimensionId,
        NavigationRouteNode start,
        NavigationDestination destination,
        List<NavigationRouteNode> nodes,
        List<NavigationTraversalEdge> edges,
        double totalEstimatedCost,
        long worldSnapshotRevision,
        long agentProfileRevision,
        long routeRevision,
        String fingerprint) {
    public NavigationRoute {
        moveId = Objects.requireNonNull(moveId, "moveId");
        backendId = Objects.requireNonNull(backendId, "backendId");
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        dimensionId = Objects.requireNonNull(dimensionId, "dimensionId");
        start = Objects.requireNonNull(start, "start");
        destination = Objects.requireNonNull(destination, "destination");
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        edges = List.copyOf(Objects.requireNonNull(edges, "edges"));
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        if (nodes.isEmpty() || totalEstimatedCost < 0.0D || worldSnapshotRevision < 0L
                || agentProfileRevision < 0L || routeRevision < 1L) {
            throw new IllegalArgumentException("Navigation route is invalid.");
        }
    }
}
