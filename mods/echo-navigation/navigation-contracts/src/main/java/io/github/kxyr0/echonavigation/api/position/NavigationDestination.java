package io.github.kxyr0.echonavigation.api.position;

import java.util.Objects;

/**
 * Authored target block. X/Z resolve to block center; Y is the NPC feet surface.
 */
public record NavigationDestination(String dimensionId, int blockX, int blockY, int blockZ) {
    public NavigationDestination {
        dimensionId = Objects.requireNonNull(dimensionId, "dimensionId").trim();
        if (dimensionId.isEmpty() || !dimensionId.contains(":")) {
            throw new IllegalArgumentException("dimensionId must be namespaced.");
        }
        if (blockY < -2048 || blockY > 4096) {
            throw new IllegalArgumentException("blockY is outside supported validation bounds.");
        }
    }

    public Vector3 standingPosition() {
        return new Vector3(blockX + 0.5D, blockY, blockZ + 0.5D);
    }

    public String blockText() {
        return blockX + "," + blockY + "," + blockZ;
    }
}
