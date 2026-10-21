package io.github.kxyr0.echonavigation.core.astar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.api.route.NavigationRouteNode;
import io.github.kxyr0.echonavigation.api.route.NavigationTraversalType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class AStarRoutePlannerTest {
    private final AStarRoutePlanner planner = new AStarRoutePlanner();

    @Test
    void straightRouteReconstructsEdges() {
        NavigationRouteNode start = node(0, 0);
        NavigationRouteNode goal = node(2, 0);
        TestGraph graph = graph(start, goal, 32, Map.of(
                start, List.of(step(node(1, 0), 1.0D)),
                node(1, 0), List.of(step(goal, 1.0D))));
        AStarSearchResult result = planner.plan(graph, 1L);
        assertEquals(AStarSearchResult.Status.ROUTE_FOUND, result.status());
        assertEquals(List.of(start, node(1, 0), goal), result.route().orElseThrow().nodes());
        assertEquals(2, result.route().orElseThrow().edges().size());
    }

    @Test
    void equalCostRouteUsesStableNodeTieBreaker() {
        NavigationRouteNode start = node(0, 0);
        NavigationRouteNode north = node(0, 1);
        NavigationRouteNode east = node(1, 0);
        NavigationRouteNode goal = node(1, 1);
        TestGraph graph = graph(start, goal, 32, Map.of(
                start, List.of(step(north, 1.0D), step(east, 1.0D)),
                north, List.of(step(goal, 1.0D)),
                east, List.of(step(goal, 1.0D))));
        AStarSearchResult first = planner.plan(graph, 1L);
        AStarSearchResult second = planner.plan(graph, 1L);
        assertEquals(first.route().orElseThrow().nodes(), second.route().orElseThrow().nodes());
        assertEquals(List.of(start, north, goal), first.route().orElseThrow().nodes());
    }

    @Test
    void blockedRouteReturnsPathNotFound() {
        TestGraph graph = graph(node(0, 0), node(1, 0), 32, Map.of());
        assertEquals(AStarSearchResult.Status.PATH_NOT_FOUND, planner.plan(graph, 1L).status());
    }

    @Test
    void boundedSearchStopsAtLimit() {
        NavigationRouteNode start = node(0, 0);
        TestGraph graph = graph(start, node(10, 0), 0, Map.of(start, List.of(step(node(1, 0), 1.0D))));
        assertEquals(AStarSearchResult.Status.SEARCH_LIMIT_REACHED, planner.plan(graph, 1L).status());
    }

    @Test
    void startEqualsDestinationCreatesSingleNodeRoute() {
        NavigationRouteNode start = node(0, 0);
        AStarSearchResult result = planner.plan(graph(start, start, 32, Map.of()), 1L);
        assertTrue(result.route().orElseThrow().edges().isEmpty());
        assertEquals(List.of(start), result.route().orElseThrow().nodes());
    }

    private AStarGraph.Step step(NavigationRouteNode target, double cost) {
        return new AStarGraph.Step(target, NavigationTraversalType.WALK, 64 * 16, 64 * 16, cost);
    }

    private NavigationRouteNode node(int x, int z) {
        return new NavigationRouteNode(x, 64, z, 0);
    }

    private TestGraph graph(NavigationRouteNode start, NavigationRouteNode goal, int limit,
            Map<NavigationRouteNode, List<AStarGraph.Step>> edges) {
        return new TestGraph(start, goal, limit, edges);
    }

    private record TestGraph(
            NavigationRouteNode start,
            NavigationRouteNode goal,
            int maximumExpandedNodes,
            Map<NavigationRouteNode, List<AStarGraph.Step>> edges) implements AStarGraph {
        @Override
        public NavigationMoveId moveId() {
            return new NavigationMoveId("test:move");
        }

        @Override
        public NavigationBackendId backendId() {
            return NavigationBackendId.ASTAR;
        }

        @Override
        public NavigationSubjectId subjectId() {
            return new NavigationSubjectId("test:npc");
        }

        @Override
        public String dimensionId() {
            return "minecraft:overworld";
        }

        @Override
        public NavigationDestination destination() {
            return new NavigationDestination(dimensionId(), goal.blockX(), goal.blockY(), goal.blockZ());
        }

        @Override
        public List<AStarGraph.Step> stepsFrom(NavigationRouteNode node) {
            return edges.getOrDefault(node, List.of());
        }

        @Override
        public double heuristic(NavigationRouteNode node) {
            return Math.abs(goal.blockX() - node.blockX()) + Math.abs(goal.blockZ() - node.blockZ());
        }

        @Override
        public long worldSnapshotRevision() {
            return 1L;
        }

        @Override
        public long agentProfileRevision() {
            return 1L;
        }
    }
}
