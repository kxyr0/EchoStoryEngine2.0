package io.github.kxyr0.echonavigation.api.route;

import io.github.kxyr0.echonavigation.api.position.Vector3;

/**
 * Surface-aware stance node. Equality uses quantized height, not raw doubles.
 */
public record NavigationRouteNode(int blockX, int blockY, int blockZ, int surfaceLevel16)
        implements Comparable<NavigationRouteNode> {
    public NavigationRouteNode {
        if (surfaceLevel16 < 0 || surfaceLevel16 > 15) {
            throw new IllegalArgumentException("surfaceLevel16 must be 0..15.");
        }
    }

    public Vector3 stancePosition() {
        return new Vector3(blockX + 0.5D, feetY(), blockZ + 0.5D);
    }

    public double feetY() {
        return blockY + surfaceLevel16 / 16.0D;
    }

    public int height16() {
        return blockY * 16 + surfaceLevel16;
    }

    @Override
    public int compareTo(NavigationRouteNode other) {
        int byX = Integer.compare(blockX, other.blockX);
        if (byX != 0) {
            return byX;
        }
        int byY = Integer.compare(blockY, other.blockY);
        if (byY != 0) {
            return byY;
        }
        int byZ = Integer.compare(blockZ, other.blockZ);
        if (byZ != 0) {
            return byZ;
        }
        return Integer.compare(surfaceLevel16, other.surfaceLevel16);
    }
}
