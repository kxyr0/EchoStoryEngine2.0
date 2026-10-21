package io.github.kxyr0.echonavigation.core.astar;

import io.github.kxyr0.echonavigation.api.route.NavigationRoute;
import io.github.kxyr0.echonavigation.api.route.NavigationRouteNode;
import io.github.kxyr0.echonavigation.api.route.NavigationTraversalEdge;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

public final class AStarRoutePlanner {
    public AStarSearchResult plan(AStarGraph graph, long routeRevision) {
        long started = System.nanoTime();
        PriorityQueue<Entry> open = new PriorityQueue<>(this::compare);
        Map<NavigationRouteNode, Entry> best = new HashMap<>();
        Entry first = new Entry(graph.start(), null, null, 0.0D, graph.heuristic(graph.start()), 0L);
        open.add(first);
        best.put(first.node(), first);
        int expanded = 0;
        int peak = 1;
        long sequence = 1L;
        while (!open.isEmpty()) {
            Entry current = open.poll();
            if (current.closed() || best.get(current.node()) != current) {
                continue;
            }
            current.close();
            if (current.node().equals(graph.goal())) {
                return found(graph, current, routeRevision, expanded, peak, started);
            }
            if (++expanded > graph.maximumExpandedNodes()) {
                return failed(AStarSearchResult.Status.SEARCH_LIMIT_REACHED,
                        "NAVIGATION_ASTAR_SEARCH_LIMIT_REACHED", expanded, peak, started);
            }
            for (AStarGraph.Step step : orderedSteps(graph, current.node())) {
                double cost = current.cost() + step.cost();
                Entry known = best.get(step.target());
                if (known == null || cost < known.cost()) {
                    double heuristic = graph.heuristic(step.target());
                    Entry next = new Entry(step.target(), current, step, cost, cost + heuristic, sequence++);
                    best.put(step.target(), next);
                    open.add(next);
                    peak = Math.max(peak, open.size());
                }
            }
        }
        return failed(AStarSearchResult.Status.PATH_NOT_FOUND, "NPC_MOVE_PATH_NOT_FOUND",
                expanded, peak, started);
    }

    private List<AStarGraph.Step> orderedSteps(AStarGraph graph, NavigationRouteNode node) {
        return graph.stepsFrom(node).stream()
                .sorted(Comparator.comparing(AStarGraph.Step::target)
                        .thenComparing(step -> step.type().ordinal())
                        .thenComparingDouble(AStarGraph.Step::cost))
                .toList();
    }

    private int compare(Entry left, Entry right) {
        int byScore = Double.compare(left.score(), right.score());
        if (byScore != 0) {
            return byScore;
        }
        int byHeuristic = Double.compare(left.heuristic(), right.heuristic());
        if (byHeuristic != 0) {
            return byHeuristic;
        }
        int byCost = Double.compare(left.cost(), right.cost());
        if (byCost != 0) {
            return byCost;
        }
        int byNode = left.node().compareTo(right.node());
        return byNode != 0 ? byNode : Long.compare(left.sequence(), right.sequence());
    }

    private AStarSearchResult found(
            AStarGraph graph,
            Entry end,
            long routeRevision,
            int expanded,
            int peak,
            long started) {
        Optional<NavigationRoute> route = reconstruct(graph, end, routeRevision);
        if (route.isEmpty()) {
            return failed(AStarSearchResult.Status.ROUTE_RECONSTRUCTION_FAILED,
                    "NAVIGATION_ASTAR_ROUTE_RECONSTRUCTION_FAILED", expanded, peak, started);
        }
        return new AStarSearchResult(AStarSearchResult.Status.ROUTE_FOUND, route,
                expanded, peak, elapsed(started), "ROUTE_FOUND");
    }

    private Optional<NavigationRoute> reconstruct(AStarGraph graph, Entry end, long routeRevision) {
        List<NavigationRouteNode> nodes = new ArrayList<>();
        List<AStarGraph.Step> steps = new ArrayList<>();
        for (Entry entry = end; entry != null; entry = entry.previous()) {
            nodes.add(0, entry.node());
            if (entry.step() != null) {
                steps.add(0, entry.step());
            }
        }
        if (nodes.isEmpty() || steps.size() != nodes.size() - 1) {
            return Optional.empty();
        }
        List<NavigationTraversalEdge> edges = new ArrayList<>();
        double total = 0.0D;
        for (int index = 0; index < steps.size(); index++) {
            AStarGraph.Step step = steps.get(index);
            total += step.cost();
            edges.add(new NavigationTraversalEdge(index, index + 1, step.type(),
                    step.entryHeight16(), step.exitHeight16(), step.cost()));
        }
        String fingerprint = graph.worldSnapshotRevision() + ":" + graph.agentProfileRevision() + ":" + nodes.hashCode();
        return Optional.of(new NavigationRoute(graph.moveId(), graph.backendId(), graph.subjectId(),
                graph.dimensionId(), nodes.get(0), graph.destination(), nodes, edges, total,
                graph.worldSnapshotRevision(), graph.agentProfileRevision(), routeRevision, fingerprint));
    }

    private AStarSearchResult failed(
            AStarSearchResult.Status status,
            String diagnostic,
            int expanded,
            int peak,
            long started) {
        return new AStarSearchResult(status, Optional.empty(), expanded, peak, elapsed(started), diagnostic);
    }

    private long elapsed(long started) {
        return Math.max(0L, System.nanoTime() - started);
    }

    private static final class Entry {
        private final NavigationRouteNode node;
        private final Entry previous;
        private final AStarGraph.Step step;
        private final double cost;
        private final double score;
        private final double heuristic;
        private final long sequence;
        private boolean closed;

        Entry(NavigationRouteNode node, Entry previous, AStarGraph.Step step,
                double cost, double score, long sequence) {
            this.node = node;
            this.previous = previous;
            this.step = step;
            this.cost = cost;
            this.score = score;
            this.heuristic = score - cost;
            this.sequence = sequence;
        }

        NavigationRouteNode node() {
            return node;
        }

        Entry previous() {
            return previous;
        }

        AStarGraph.Step step() {
            return step;
        }

        double cost() {
            return cost;
        }

        double score() {
            return score;
        }

        double heuristic() {
            return heuristic;
        }

        long sequence() {
            return sequence;
        }

        boolean closed() {
            return closed;
        }

        void close() {
            closed = true;
        }
    }
}
