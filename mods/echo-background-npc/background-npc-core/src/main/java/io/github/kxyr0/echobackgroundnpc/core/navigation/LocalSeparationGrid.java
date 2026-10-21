package io.github.kxyr0.echobackgroundnpc.core.navigation;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Spatial hash limiting avoidance work to adjacent cells instead of all-agent O(N²). */
public final class LocalSeparationGrid {
    private final double cellSize;
    private final Map<Cell, List<AgentPoint>> cells = new HashMap<>();

    public LocalSeparationGrid(double cellSize) {
        if (!Double.isFinite(cellSize) || cellSize <= 0.0) throw new IllegalArgumentException("Invalid cell size.");
        this.cellSize = cellSize;
    }

    public void rebuild(List<AgentPoint> agents) {
        cells.clear();
        agents.forEach(agent -> cells.computeIfAbsent(cell(agent.point()), unused -> new ArrayList<>()).add(agent));
    }

    public List<AgentPoint> neighbors(WorldPoint point) {
        Cell center = cell(point);
        List<AgentPoint> result = new ArrayList<>();
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            result.addAll(cells.getOrDefault(new Cell(center.dimension(), center.x() + x, center.z() + z), List.of()));
        }
        return List.copyOf(result);
    }

    private Cell cell(WorldPoint point) {
        return new Cell(point.dimension(), (int) Math.floor(point.x() / cellSize),
                (int) Math.floor(point.z() / cellSize));
    }

    public record AgentPoint(BackgroundNpcId id, WorldPoint point) { }
    private record Cell(String dimension, int x, int z) { }
}
