package io.github.kxyr0.echonavigation.vanilla.astar.surface;

import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import io.github.kxyr0.echonavigation.api.route.NavigationRouteNode;
import io.github.kxyr0.echonavigation.core.astar.AStarGraph;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import io.github.kxyr0.echonavigation.forge.api.service.NavigationSurfaceRouteService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AStarWorldSnapshotFactory {
    private static final int HORIZONTAL_MARGIN = 12;
    private static final int VERTICAL_MARGIN = 8;
    private static final int MAX_EXPANDED_NODES = 32768;
    private static final int MAX_SNAPSHOT_VOLUME = 120000;

    public Result create(NavigationSnapshot move, NavigationAgentHandle handle) {
        var profile = NavigationAgentProfile.from(handle);
        var request = new NavigationSurfaceRouteService.PlanRequest(move.moveId(), move.subjectId(), handle.level(),
                new io.github.kxyr0.echonavigation.api.position.Vector3(handle.mob().getX(), handle.mob().getY(),
                        handle.mob().getZ()), move.destination(), new NavigationSurfaceRouteService.AgentProfile(
                        profile.width(), profile.height(), profile.eyeHeight(), profile.maximumStepUp(),
                        profile.maximumControlledDrop(), profile.movementSpeed(), profile.canUseLadders(),
                        profile.canOpenWoodenDoors(), profile.revision()), move.progress().repathCount() + 1L);
        return create(request);
    }

    public Result create(NavigationSurfaceRouteService.PlanRequest request) {
        NavigationAgentProfile profile = NavigationAgentProfile.from(request.profile());
        NavigationSurfaceSampler sampler = new NavigationSurfaceSampler(request.level(), profile);
        Optional<NavigationRouteNode> start = sampler.nearestSurface(request.start().x(), request.start().y(),
                request.start().z());
        if (start.isEmpty()) {
            return Result.failed("NAVIGATION_ASTAR_NO_START_SURFACE");
        }
        Optional<NavigationRouteNode> goal = sampler.destinationSurface(request.destination());
        if (goal.isEmpty()) {
            return Result.failed("NAVIGATION_ASTAR_NO_DESTINATION_SURFACE");
        }
        NavigationSurfaceBounds bounds = NavigationSurfaceBounds.around(start.orElseThrow(), goal.orElseThrow(),
                HORIZONTAL_MARGIN, VERTICAL_MARGIN);
        if (bounds.volume() > MAX_SNAPSHOT_VOLUME) {
            return Result.failed("NAVIGATION_ASTAR_SEARCH_LIMIT_REACHED");
        }
        SnapshotCells cells = cells(bounds, sampler, start.orElseThrow(), goal.orElseThrow());
        Map<NavigationRouteNode, List<AStarGraph.Step>> edges = edges(cells, sampler, profile);
        long worldRevision = request.level().getGameTime();
        var snapshot = new AStarWorldSnapshot(request.moveId(), request.subjectId(),
                request.destination().dimensionId(), request.destination(), start.orElseThrow(), goal.orElseThrow(),
                edges, MAX_EXPANDED_NODES,
                worldRevision, profile.revision(), bounds, cells.nodes().size());
        return new Result(Optional.of(snapshot), "SNAPSHOT_READY");
    }

    private SnapshotCells cells(NavigationSurfaceBounds bounds, NavigationSurfaceSampler sampler,
            NavigationRouteNode start, NavigationRouteNode goal) {
        Map<Column, List<NavigationRouteNode>> columns = new LinkedHashMap<>();
        List<NavigationRouteNode> nodes = new ArrayList<>();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int y = bounds.minY() - 1; y <= bounds.maxY(); y++) {
                    add(nodes, columns, sampler.surfaceNodes(x, z, y), bounds);
                    sampler.climbableNode(x, y, z).ifPresent(node -> add(nodes, columns, List.of(node), bounds));
                }
            }
        }
        add(nodes, columns, List.of(start, goal), bounds);
        nodes.sort(Comparator.naturalOrder());
        return new SnapshotCells(List.copyOf(nodes), columns);
    }

    private void add(List<NavigationRouteNode> nodes, Map<Column, List<NavigationRouteNode>> columns,
            List<NavigationRouteNode> candidates, NavigationSurfaceBounds bounds) {
        for (NavigationRouteNode node : candidates) {
            if (bounds.contains(node) && !nodes.contains(node)) {
                nodes.add(node);
                columns.computeIfAbsent(new Column(node.blockX(), node.blockZ()), ignored -> new ArrayList<>())
                        .add(node);
            }
        }
    }

    private Map<NavigationRouteNode, List<AStarGraph.Step>> edges(
            SnapshotCells cells,
            NavigationSurfaceSampler sampler,
            NavigationAgentProfile profile) {
        NavigationEdgeFactory factory = new NavigationEdgeFactory(sampler, profile);
        Map<NavigationRouteNode, List<AStarGraph.Step>> result = new LinkedHashMap<>();
        for (NavigationRouteNode node : cells.nodes()) {
            List<AStarGraph.Step> steps = new ArrayList<>(factory.horizontalSteps(node,
                    (x, z) -> cells.columns().getOrDefault(new Column(x, z), List.of())));
            for (NavigationRouteNode target : cells.columns().getOrDefault(new Column(node.blockX(), node.blockZ()),
                    List.of())) {
                factory.ladderStep(node, target).ifPresent(steps::add);
            }
            if (!steps.isEmpty()) {
                result.put(node, List.copyOf(steps));
            }
        }
        return result;
    }

    public record Result(Optional<AStarWorldSnapshot> snapshot, String diagnostic) {
        static Result failed(String diagnostic) {
            return new Result(Optional.empty(), diagnostic);
        }
    }

    private record SnapshotCells(List<NavigationRouteNode> nodes, Map<Column, List<NavigationRouteNode>> columns) {
    }

    private record Column(int x, int z) {
    }
}
