package io.github.kxyr0.echonavigation.vanilla.astar.surface;

import io.github.kxyr0.echonavigation.api.route.NavigationRouteNode;

public record NavigationSurfaceBounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
    public static NavigationSurfaceBounds around(NavigationRouteNode start, NavigationRouteNode goal,
            int horizontalMargin, int verticalMargin) {
        return new NavigationSurfaceBounds(
                Math.min(start.blockX(), goal.blockX()) - horizontalMargin,
                Math.max(start.blockX(), goal.blockX()) + horizontalMargin,
                Math.min(start.blockY(), goal.blockY()) - verticalMargin,
                Math.max(start.blockY(), goal.blockY()) + verticalMargin,
                Math.min(start.blockZ(), goal.blockZ()) - horizontalMargin,
                Math.max(start.blockZ(), goal.blockZ()) + horizontalMargin);
    }

    public boolean contains(NavigationRouteNode node) {
        return node.blockX() >= minX && node.blockX() <= maxX
                && node.blockY() >= minY && node.blockY() <= maxY
                && node.blockZ() >= minZ && node.blockZ() <= maxZ;
    }

    public int volume() {
        return Math.max(0, maxX - minX + 1) * Math.max(0, maxY - minY + 1)
                * Math.max(0, maxZ - minZ + 1);
    }
}
