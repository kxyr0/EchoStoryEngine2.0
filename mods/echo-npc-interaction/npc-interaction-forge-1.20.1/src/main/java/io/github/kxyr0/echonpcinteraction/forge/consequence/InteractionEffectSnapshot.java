package io.github.kxyr0.echonpcinteraction.forge.consequence;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record InteractionEffectSnapshot(
        String targetStateFingerprint,
        String targetBlockEntityFingerprint,
        String nearbyStateFingerprint,
        Map<UUID, String> entities,
        Optional<UUID> proxyVehicleId,
        Optional<String> proxyVehicleType,
        boolean menuOpen,
        String inventoryFingerprint,
        Vec3 proxyPosition,
        String proxyPose) {
    public InteractionEffectSnapshot {
        targetStateFingerprint = Objects.requireNonNull(targetStateFingerprint, "targetStateFingerprint");
        targetBlockEntityFingerprint = Objects.requireNonNull(targetBlockEntityFingerprint,
                "targetBlockEntityFingerprint");
        nearbyStateFingerprint = Objects.requireNonNull(nearbyStateFingerprint, "nearbyStateFingerprint");
        entities = Map.copyOf(Objects.requireNonNull(entities, "entities"));
        proxyVehicleId = Objects.requireNonNull(proxyVehicleId, "proxyVehicleId");
        proxyVehicleType = Objects.requireNonNull(proxyVehicleType, "proxyVehicleType");
        inventoryFingerprint = Objects.requireNonNull(inventoryFingerprint, "inventoryFingerprint");
        proxyPosition = Objects.requireNonNull(proxyPosition, "proxyPosition");
        proxyPose = Objects.requireNonNull(proxyPose, "proxyPose");
    }

    public Set<UUID> entityIds() {
        return entities.keySet();
    }

    public Set<UUID> spawnedSince(InteractionEffectSnapshot before) {
        java.util.LinkedHashSet<UUID> ids = new java.util.LinkedHashSet<>(entityIds());
        ids.removeAll(before.entityIds());
        return Set.copyOf(ids);
    }

    public Optional<Entity> proxyVehicle(ServerLevel level) {
        return proxyVehicleId.flatMap(id -> Optional.ofNullable(level.getEntity(id)));
    }
}
