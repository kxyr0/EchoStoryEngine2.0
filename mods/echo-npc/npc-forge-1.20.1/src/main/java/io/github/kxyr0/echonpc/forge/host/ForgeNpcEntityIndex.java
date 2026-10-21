package io.github.kxyr0.echonpc.forge.host;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.core.platform.PlatformActorHandle;
import io.github.kxyr0.echonpc.forge.entity.EchoNpcEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Server-host-owned entity ownership index. It is discarded when the host stops.
 */
final class ForgeNpcEntityIndex {
    private final Map<UUID, ActorId> owners = new HashMap<>();
    private final Map<ActorId, PlatformActorHandle> handles = new HashMap<>();

    PlatformActorHandle claim(EchoNpcEntity entity, ActorId actorId) {
        owners.put(entity.getUUID(), actorId);
        PlatformActorHandle handle = new PlatformActorHandle(entity.getUUID().toString());
        handles.put(actorId, handle);
        return handle;
    }

    void release(PlatformActorHandle handle) {
        ActorId actorId = owners.remove(UUID.fromString(handle.value()));
        if (actorId != null) {
            handles.remove(actorId);
        }
    }

    void releaseActor(ActorId actorId) {
        PlatformActorHandle handle = handles.remove(actorId);
        if (handle != null) {
            owners.remove(UUID.fromString(handle.value()));
        }
    }

    Optional<PlatformActorHandle> handle(ActorId actorId) {
        return Optional.ofNullable(handles.get(actorId));
    }

    Optional<EchoNpcEntity> find(MinecraftServer server, PlatformActorHandle handle) {
        UUID uuid = UUID.fromString(handle.value());
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(uuid) instanceof EchoNpcEntity entity) {
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    boolean owns(PlatformActorHandle handle) {
        return owners.containsKey(UUID.fromString(handle.value()));
    }

    void clear() {
        owners.clear();
        handles.clear();
    }
}
