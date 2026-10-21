package io.github.kxyr0.echobackgroundnpc.forge.runtime;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.runtime.BackgroundAgentState;
import io.github.kxyr0.echobackgroundnpc.api.runtime.MaterializationState;
import io.github.kxyr0.echobackgroundnpc.core.navigation.WorldPoint;
import io.github.kxyr0.echobackgroundnpc.forge.entity.BackgroundNpcEntities;
import io.github.kxyr0.echobackgroundnpc.forge.entity.BackgroundNpcEntity;
import io.github.kxyr0.echobackgroundnpc.forge.api.event.BackgroundNpcMaterializationEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

/** Owns at most one server Entity shell per logical agent, shared by all viewers. */
public final class MaterializedBodies {
    private final MinecraftServer server;
    private final Map<BackgroundNpcId, UUID> entities = new HashMap<>();

    public MaterializedBodies(MinecraftServer server) { this.server = server; }

    public Optional<BackgroundNpcEntity> find(BackgroundNpcId id) {
        UUID uuid = entities.get(id);
        if (uuid == null) return Optional.empty();
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(uuid) instanceof BackgroundNpcEntity entity && !entity.isRemoved()) {
                return Optional.of(entity);
            }
        }
        entities.remove(id);
        return Optional.empty();
    }

    public boolean materialize(BackgroundAgentState state, WorldPoint point) {
        if (find(state.id()).isPresent()) return true;
        ServerLevel level = level(point.dimension());
        if (level == null || !level.hasChunkAt(net.minecraft.core.BlockPos.containing(point.x(), point.y(), point.z()))) {
            return false;
        }
        BackgroundNpcEntity entity = BackgroundNpcEntities.BACKGROUND_NPC.get().create(level);
        if (entity == null) return false;
        entity.configure(state);
        entity.setPos(point.x(), point.y(), point.z());
        if (!level.addFreshEntity(entity)) return false;
        entities.put(state.id(), entity.getUUID());
        MinecraftForge.EVENT_BUS.post(new BackgroundNpcMaterializationEvent(state.id(),
                MaterializationState.MATERIALIZED));
        return true;
    }

    public void dematerialize(BackgroundNpcId id) {
        find(id).ifPresent(Entity -> Entity.discard());
        if (entities.remove(id) != null) MinecraftForge.EVENT_BUS.post(new BackgroundNpcMaterializationEvent(id,
                MaterializationState.VIRTUAL));
    }

    public double nearestPlayerDistance(WorldPoint point) {
        ServerLevel level = level(point.dimension());
        if (level == null) return Double.POSITIVE_INFINITY;
        return server.getPlayerList().getPlayers().stream().filter(player -> player.serverLevel() == level)
                .mapToDouble(player -> Math.sqrt(player.distanceToSqr(point.x(), point.y(), point.z())))
                .min().orElse(Double.POSITIVE_INFINITY);
    }

    public int count() { return entities.size(); }
    public void clear() { ListCopy.ids(entities).forEach(this::dematerialize); }

    private ServerLevel level(String dimension) {
        ResourceLocation id = ResourceLocation.tryParse(dimension);
        return id == null ? null : server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                id));
    }

    private static final class ListCopy {
        private ListCopy() { }
        static java.util.List<BackgroundNpcId> ids(Map<BackgroundNpcId, UUID> values) {
            return java.util.List.copyOf(values.keySet());
        }
    }
}
