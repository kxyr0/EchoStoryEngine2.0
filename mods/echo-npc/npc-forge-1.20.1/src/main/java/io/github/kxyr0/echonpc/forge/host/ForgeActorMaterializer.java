package io.github.kxyr0.echonpc.forge.host;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyDimensions;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormDefinition;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.actor.ActiveActor;
import io.github.kxyr0.echonpc.core.actor.EntityBindingId;
import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import io.github.kxyr0.echonpc.core.platform.PlatformActorGateway;
import io.github.kxyr0.echonpc.core.platform.PlatformActorHandle;
import io.github.kxyr0.echonpc.forge.entity.EchoNpcEntity;
import io.github.kxyr0.echonpc.forge.registry.EchoNpcEntities;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

public final class ForgeActorMaterializer implements PlatformActorGateway {
    private final MinecraftServer server;
    private final ForgeNpcEntityIndex index;

    public ForgeActorMaterializer(MinecraftServer server, ForgeNpcEntityIndex index) {
        this.server = java.util.Objects.requireNonNull(server, "server");
        this.index = java.util.Objects.requireNonNull(index, "index");
    }

    public Result materialize(ActorDesiredState desired, NpcDefinition definition,
            Optional<PlatformActorHandle> previous) {
        return materialize(desired, definition, Optional.empty(), previous);
    }

    public Result materialize(ActorDesiredState desired, NpcDefinition definition,
            Optional<NpcFormDefinition> form, Optional<PlatformActorHandle> previous) {
        if (!chunkLoaded(desired.worldId(), desired.position())) {
            return new Result(NpcActorView.Status.DORMANT_CHUNK_UNLOADED, Optional.empty(),
                    "Target chunk не загружен.");
        }
        if (previous.isPresent() && exists(previous.orElseThrow())) {
            MoveResult moved = teleport(previous.orElseThrow(), desired.worldId(), desired.position(),
                    desired.rotation());
            if (moved.status() == Status.ACCEPTED) {
                previous.flatMap(this::findEntity).ifPresent(entity -> entity.configure(desired.actorId(),
                        desired.entityBindingId(), definition, form, desired.activeSkinId()));
                previous.flatMap(this::findEntity).ifPresent(entity -> syncRestState(entity, desired));
                return new Result(NpcActorView.Status.MATERIALIZED, previous, moved.message());
            }
            despawn(previous.orElseThrow());
        }
        Optional<PlatformActorHandle> adopted = adoptSingle(desired, definition, form);
        if (adopted.isPresent()) {
            return new Result(NpcActorView.Status.MATERIALIZED, adopted, "Entity adopted.");
        }
        SpawnResult spawned = spawn(desired, definition, form);
        return new Result(spawned.status() == Status.ACCEPTED ? NpcActorView.Status.MATERIALIZED
                : NpcActorView.Status.PLATFORM_FAILURE, spawned.handle(), spawned.message());
    }

    public void removeActor(ActorId actorId, Optional<PlatformActorHandle> handle) {
        handle.ifPresent(this::despawn);
        index.releaseActor(actorId);
    }

    public Optional<PlatformActorHandle> handle(ActorId actorId) { return index.handle(actorId); }
    public Optional<EchoNpcEntity> findEntity(PlatformActorHandle handle) { return index.find(server, handle); }

    public PlatformActorHandle adopt(EchoNpcEntity entity, ActorDesiredState desired,
            NpcDefinition definition, Optional<NpcFormDefinition> form) {
        entity.configure(desired.actorId(), desired.entityBindingId(), definition, form, desired.activeSkinId());
        return index.claim(entity, desired.actorId());
    }

    @Override
    public boolean worldExists(NpcWorldId worldId) { return server.getLevel(ForgeWorldIds.toKey(worldId)) != null; }
    @Override
    public boolean chunkLoaded(NpcWorldId worldId, NpcPosition position) {
        ServerLevel level = server.getLevel(ForgeWorldIds.toKey(worldId));
        return level != null && level.hasChunkAt(BlockPos.containing(position.x(), position.y(), position.z()));
    }

    @Override
    public SpawnResult spawn(ActorDesiredState desired, NpcDefinition definition) {
        return spawn(desired, definition, Optional.empty()); }

    public SpawnResult spawn(ActorDesiredState desired, NpcDefinition definition, Optional<NpcFormDefinition> form) {
        ServerLevel level = server.getLevel(ForgeWorldIds.toKey(desired.worldId()));
        if (level == null) {
            return new SpawnResult(Status.MISSING, Optional.empty(), "Мир не найден.");
        }
        EchoNpcEntity entity = EchoNpcEntities.ECHO_NPC.get().create(level);
        if (entity == null) {
            return new SpawnResult(Status.REJECTED, Optional.empty(), "Entity factory вернула null.");
        }
        entity.configure(desired.actorId(), desired.entityBindingId(), definition, form, desired.activeSkinId());
        entity.moveTo(desired.position().x(), desired.position().y(), desired.position().z(),
                desired.rotation().yaw(), desired.rotation().pitch());
        return addEntity(level, entity, desired.actorId());
    }

    @Override
    public MoveResult teleport(PlatformActorHandle handle, NpcWorldId worldId,
            NpcPosition position, NpcRotation rotation) {
        Optional<EchoNpcEntity> entity = index.find(server, handle);
        ServerLevel level = server.getLevel(ForgeWorldIds.toKey(worldId));
        if (entity.isEmpty() || level == null) {
            return new MoveResult(Status.MISSING, "Entity или мир не найден.");
        }
        if (entity.orElseThrow().level() != level) {
            return new MoveResult(Status.REJECTED, "Entity находится в другом dimension.");
        }
        entity.orElseThrow().moveTo(position.x(), position.y(), position.z(), rotation.yaw(), rotation.pitch());
        return new MoveResult(Status.ACCEPTED, "Entity перемещена.");
    }

    @Override
    public RemoveResult despawn(PlatformActorHandle handle) {
        Optional<EchoNpcEntity> entity = index.find(server, handle);
        index.release(handle);
        entity.ifPresent(EchoNpcEntity::discard);
        return new RemoveResult(Status.ACCEPTED, "Entity удалена или уже отсутствовала.");
    }

    @Override
    public boolean exists(PlatformActorHandle handle) {
        return index.owns(handle) && index.find(server, handle).isPresent(); }

    public boolean formFits(ActiveActor actor, NpcFormDefinition form) {
        Optional<EchoNpcEntity> entity = actor.handle().flatMap(this::findEntity);
        return entity.map(value -> noCollision(value, form.dimensions())).orElse(false);
    }

    public boolean targetFits(NpcWorldId worldId, NpcPosition position,
            Optional<NpcFormDefinition> form, Optional<PlatformActorHandle> handle) {
        ServerLevel level = server.getLevel(ForgeWorldIds.toKey(worldId));
        if (level == null) {
            return false;
        }
        Optional<EchoNpcEntity> entity = handle.flatMap(this::findEntity);
        return level.noCollision(entity.orElse(null), box(position, form.map(NpcFormDefinition::dimensions)
                .orElse(new NpcBodyDimensions(0.6D, 1.8D, 1.62D))));
    }

    public boolean navigationInProgress(Optional<PlatformActorHandle> handle) {
        return handle.flatMap(this::findEntity).map(entity -> entity.getMoveControl().hasWanted()).orElse(false);
    }

    private Optional<PlatformActorHandle> adoptSingle(ActorDesiredState desired, NpcDefinition definition,
            Optional<NpcFormDefinition> form) {
        List<EchoNpcEntity> linked = candidates(desired).stream()
                .filter(entity -> linkedToDesired(entity, desired))
                .toList();
        List<EchoNpcEntity> exact = linked.stream().filter(entity -> exactLink(entity, desired)).toList();
        if (exact.size() == 1) {
            EchoNpcEntity entity = exact.get(0);
            entity.configure(desired.actorId(), desired.entityBindingId(), definition, form, desired.activeSkinId());
            syncRestState(entity, desired);
            return Optional.of(index.claim(entity, desired.actorId()));
        }
        linked.forEach(EchoNpcEntity::discard);
        return Optional.empty();
    }

    private boolean linkedToDesired(EchoNpcEntity entity, ActorDesiredState desired) {
        return desired.actorId().value().equals(entity.actorIdValue())
                || desired.entityBindingId().value().equals(entity.bindingIdValue());
    }

    private boolean exactLink(EchoNpcEntity entity, ActorDesiredState desired) {
        return desired.actorId().value().equals(entity.actorIdValue())
                && desired.entityBindingId().value().equals(entity.bindingIdValue());
    }

    private List<EchoNpcEntity> candidates(ActorDesiredState desired) {
        ServerLevel level = server.getLevel(ForgeWorldIds.toKey(desired.worldId()));
        if (level == null) {
            return List.of();
        }
        NpcPosition pos = desired.position();
        AABB box = new AABB(pos.x(), pos.y(), pos.z(), pos.x(), pos.y(), pos.z()).inflate(4.0D);
        return level.getEntitiesOfClass(EchoNpcEntity.class, box);
    }

    private SpawnResult addEntity(ServerLevel level, EchoNpcEntity entity, ActorId actorId) {
        if (!level.addFreshEntity(entity)) {
            return new SpawnResult(Status.REJECTED, Optional.empty(), "Minecraft отверг spawn entity.");
        }
        return new SpawnResult(Status.ACCEPTED, Optional.of(index.claim(entity, actorId)), "Entity создана.");
    }

    private void syncRestState(EchoNpcEntity entity, ActorDesiredState desired) {
        if (desired.restAttachment().isEmpty()) {
            entity.clearSleeping();
        }
    }

    private boolean noCollision(EchoNpcEntity entity, NpcBodyDimensions dimensions) {
        return entity.level().noCollision(entity, box(new NpcPosition(entity.getX(), entity.getY(), entity.getZ()),
                dimensions));
    }

    private AABB box(NpcPosition position, NpcBodyDimensions dimensions) {
        double half = dimensions.width() * 0.5D;
        return new AABB(position.x() - half, position.y(), position.z() - half,
                position.x() + half, position.y() + dimensions.height(), position.z() + half);
    }

    public record Result(NpcActorView.Status status, Optional<PlatformActorHandle> handle, String message) {
        public Result {
            status = java.util.Objects.requireNonNull(status, "status");
            handle = java.util.Objects.requireNonNull(handle, "handle");
            message = java.util.Objects.requireNonNull(message, "message");
        }
    }
}
