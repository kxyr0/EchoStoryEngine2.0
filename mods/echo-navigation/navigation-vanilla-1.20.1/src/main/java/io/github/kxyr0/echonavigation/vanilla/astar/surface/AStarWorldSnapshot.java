package io.github.kxyr0.echonavigation.vanilla.astar.surface;

import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.api.route.NavigationRouteNode;
import io.github.kxyr0.echonavigation.core.astar.AStarGraph;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AStarWorldSnapshot(
        NavigationMoveId moveId,
        NavigationSubjectId subjectId,
        String dimensionId,
        NavigationDestination destination,
        NavigationRouteNode start,
        NavigationRouteNode goal,
        Map<NavigationRouteNode, List<AStarGraph.Step>> edges,
        int maximumExpandedNodes,
        long worldSnapshotRevision,
        long agentProfileRevision,
        NavigationSurfaceBounds bounds,
        int surfaceNodeCount) implements AStarGraph {
    public AStarWorldSnapshot {
        moveId = Objects.requireNonNull(moveId, "moveId");
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        dimensionId = Objects.requireNonNull(dimensionId, "dimensionId");
        destination = Objects.requireNonNull(destination, "destination");
        start = Objects.requireNonNull(start, "start");
        goal = Objects.requireNonNull(goal, "goal");
        edges = Map.copyOf(Objects.requireNonNull(edges, "edges"));
        bounds = Objects.requireNonNull(bounds, "bounds");
    }

    @Override
    public NavigationBackendId backendId() {
        return NavigationBackendId.ASTAR;
    }

    @Override
    public List<AStarGraph.Step> stepsFrom(NavigationRouteNode node) {
        return edges.getOrDefault(node, List.of());
    }

    @Override
    public double heuristic(NavigationRouteNode node) {
        int dx = Math.abs(goal.blockX() - node.blockX());
        int dz = Math.abs(goal.blockZ() - node.blockZ());
        double vertical = Math.abs(goal.height16() - node.height16()) / 16.0D;
        return dx + dz + vertical * 0.1D;
    }
}
