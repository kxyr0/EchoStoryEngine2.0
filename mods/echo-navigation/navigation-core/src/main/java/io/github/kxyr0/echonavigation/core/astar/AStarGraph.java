package io.github.kxyr0.echonavigation.core.astar;

import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.api.route.NavigationRouteNode;
import io.github.kxyr0.echonavigation.api.route.NavigationTraversalType;
import java.util.List;

public interface AStarGraph {
    NavigationMoveId moveId();

    NavigationBackendId backendId();

    NavigationSubjectId subjectId();

    String dimensionId();

    NavigationDestination destination();

    NavigationRouteNode start();

    NavigationRouteNode goal();

    List<Step> stepsFrom(NavigationRouteNode node);

    double heuristic(NavigationRouteNode node);

    int maximumExpandedNodes();

    long worldSnapshotRevision();

    long agentProfileRevision();

    record Step(
            NavigationRouteNode target,
            NavigationTraversalType type,
            int entryHeight16,
            int exitHeight16,
            double cost) {
    }
}
