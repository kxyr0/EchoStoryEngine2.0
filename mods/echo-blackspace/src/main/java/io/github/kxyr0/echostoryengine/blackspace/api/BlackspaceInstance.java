package io.github.kxyr0.echostoryengine.blackspace.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public record BlackspaceInstance(
        String id,
        ResourceKey<Level> dimension,
        BlockPos platformCenter,
        double spawnX,
        double spawnY,
        double spawnZ,
        float yaw,
        float pitch,
        int platformRadius,
        double visualRadius,
        double verticalRadius) {
    public BlackspaceInstance {
        id = requireText(id, "id");
        dimension = Objects.requireNonNull(dimension, "dimension");
        platformCenter = Objects.requireNonNull(platformCenter, "platformCenter");
        if (platformRadius < 1) {
            throw new IllegalArgumentException("platformRadius must be positive");
        }
        if (visualRadius <= 0.0D || verticalRadius <= 0.0D) {
            throw new IllegalArgumentException("visual and vertical radii must be positive");
        }
    }

    public String dimensionId() {
        return dimension.location().toString();
    }

    public String spawnValue() {
        return dimensionId() + ";" + spawnX + ";" + spawnY + ";" + spawnZ + ";" + yaw + ";" + pitch;
    }

    public Map<String, String> outputs() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("instanceId", id);
        values.put("dimension", dimensionId());
        values.put("spawnX", Double.toString(spawnX));
        values.put("spawnY", Double.toString(spawnY));
        values.put("spawnZ", Double.toString(spawnZ));
        values.put("yaw", Float.toString(yaw));
        values.put("pitch", Float.toString(pitch));
        values.put("Spawn", spawnValue());
        return Map.copyOf(values);
    }

    public boolean contains(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!player.level().dimension().equals(dimension)) {
            return false;
        }
        double dx = Math.abs(player.getX() - platformCenter.getX());
        double dz = Math.abs(player.getZ() - platformCenter.getZ());
        double dy = Math.abs(player.getY() - platformCenter.getY());
        return dx <= visualRadius && dz <= visualRadius && dy <= verticalRadius;
    }

    public boolean isPlatformBlock(BlockPos pos) {
        Objects.requireNonNull(pos, "pos");
        if (pos.getY() != platformCenter.getY()) {
            return false;
        }
        return Math.abs(pos.getX() - platformCenter.getX()) <= platformRadius
                && Math.abs(pos.getZ() - platformCenter.getZ()) <= platformRadius;
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
