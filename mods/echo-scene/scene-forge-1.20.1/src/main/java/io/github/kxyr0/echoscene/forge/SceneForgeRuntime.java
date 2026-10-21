package io.github.kxyr0.echoscene.forge;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorStatus;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlLease;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlMode;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlStatus;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerControlLeaseId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerControlScopeId;
import io.github.kxyr0.echoplayeractor.forge.api.service.EchoPlayerActorServiceBridge;
import io.github.kxyr0.echoscene.api.definition.SceneCapability;
import io.github.kxyr0.echoscene.api.definition.SceneCapabilityKind;
import io.github.kxyr0.echoscene.api.definition.SceneDefinition;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.api.model.SceneInspection;
import io.github.kxyr0.echoscene.api.port.SceneCheckpointStore;
import io.github.kxyr0.echoscene.api.port.SceneLeasePort;
import io.github.kxyr0.echoscene.forge.command.SceneCommandHub;
import io.github.kxyr0.echoscene.runtime.SceneCheckpoint;
import io.github.kxyr0.echoscene.runtime.SceneRuntime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.level.ServerPlayer;

final class SceneForgeRuntime {
    private final io.github.kxyr0.echoscene.forge.persistence.SceneDomainPersistence persistence =
            new io.github.kxyr0.echoscene.forge.persistence.SceneDomainPersistence();
    private final Map<SceneExecutionId, ServerPlayer> players = new LinkedHashMap<>();
    private final Map<SceneExecutionId, SceneCheckpoint> checkpoints = new LinkedHashMap<>();
    private final Map<SceneCapability, Lease> leases = new LinkedHashMap<>();
    private final SceneCommandHub commands;
    private final SceneRuntime runtime;

    SceneForgeRuntime(SceneForgeCatalog catalog) {
        commands = new SceneCommandHub(players::get, catalog::location);
        runtime = new SceneRuntime(commands, new Leases(), commands::restoreCamera, new Checkpoints(), this::changed);
        commands.bind(runtime::complete, new SceneCommandHub.NestedScenes() {
            public Optional<SceneDefinition> find(String resourceId) {
                return catalog.find(resourceId).flatMap(value -> value.definition());
            }
            public SceneInspection start(SceneDefinition definition, ServerPlayer player, String rootOwner) {
                return playOwned(definition, player, rootOwner);
            }
            public Optional<SceneInspection> inspect(SceneExecutionId executionId) {
                return runtime.inspect(executionId);
            }
            public void cancel(SceneExecutionId executionId) { runtime.cancel(executionId); }
        });
    }

    SceneInspection play(SceneDefinition definition, ServerPlayer player) {
        SceneExecutionId id = nextId();
        return play(definition, player, id, id.value());
    }

    SceneInspection play(SceneDefinition definition, ServerPlayer player, SceneExecutionId id, String root) {
        var existing = runtime.inspect(id);
        if (existing.isPresent()) {
            if (!existing.get().resourceId().equals(definition.resourceId())) throw new IllegalArgumentException("Scene execution identity conflict");
            return existing.get();
        }
        var saved = persistence.find(id);
        if (saved.isPresent() && (!saved.get().player().equals(player.getUUID())
                || !saved.get().definition().resourceId().equals(definition.resourceId()))) {
            throw new IllegalArgumentException("Scene execution audience/resource conflict");
        }
        players.put(id, player);
        if (saved.isPresent()) return runtime.recover(saved.get().definition(), saved.get().checkpoint());
        persistence.register(definition, id, player.getUUID());
        return runtime.start(definition, id, root);
    }
    void initialize(net.minecraft.server.MinecraftServer server) { persistence.initialize(server, runtime); }
    void recoverPlayer(ServerPlayer player) {
        persistence.recoverPlayer(player, runtime, players);
    }
    void tick() { commands.tick(); }
    List<SceneInspection> running() { return runtime.running(); }
    Optional<SceneInspection> inspect(SceneExecutionId id) { return runtime.inspect(id); }

    SceneInspection cancel(SceneExecutionId id) {
        SceneInspection result = runtime.cancel(id);
        players.remove(id);
        return result;
    }

    private SceneInspection playOwned(SceneDefinition definition, ServerPlayer player, String rootOwner) {
        SceneExecutionId id = nextId();
        return play(definition, player, id, rootOwner);
    }

    private SceneExecutionId nextId() {
        return new SceneExecutionId("SceneExecution#" + java.util.UUID.randomUUID());
    }

    private void changed(SceneInspection value) {
        if (value.state().terminal()) players.remove(value.executionId());
    }

    private final class Leases implements SceneLeasePort {
        public LeaseResult acquire(SceneExecutionId owner, String rootOwner, SceneCapability capability) {
            Lease current = leases.get(capability);
            if (current != null && !current.rootOwner.equals(rootOwner)) {
                return LeaseResult.refused("held by " + current.owners.iterator().next().value());
            }
            if (current != null) {
                current.owners.add(owner);
                return LeaseResult.granted();
            }
            ServerPlayer player = players.get(owner);
            String failure = capability.kind() == SceneCapabilityKind.PLAYER
                    ? acquirePlayer(rootOwner, player) : "";
            if (!failure.isEmpty()) return LeaseResult.refused(failure);
            Lease created = new Lease(rootOwner, player);
            created.owners.add(owner);
            leases.put(capability, created);
            return LeaseResult.granted();
        }

        public void releaseOwned(SceneExecutionId owner) {
            List<SceneCapability> released = leases.entrySet().stream().filter(entry -> {
                entry.getValue().owners.remove(owner);
                return entry.getValue().owners.isEmpty();
            }).map(Map.Entry::getKey).toList();
            released.forEach(capability -> {
                Lease lease = leases.remove(capability);
                if (capability.kind() == SceneCapabilityKind.PLAYER) releasePlayer(lease);
            });
        }

        private String acquirePlayer(String rootOwner, ServerPlayer player) {
            if (player == null) return "player is unavailable";
            var lookup = EchoPlayerActorServiceBridge.lookup(player.getServer());
            if (lookup.leaseService().isEmpty()) return "Player Actor lease service unavailable";
            String token = "echo_scene_" + SceneCommandHub.token(rootOwner);
            PlayerControlLease lease = new PlayerControlLease(new PlayerControlLeaseId(token),
                    PlayerActorId.of(player.getUUID()), PlayerControlScopeId.CINEMATIC_CONTROL,
                    PlayerControlMode.CINEMATIC_CONTROL, playerOwner(rootOwner), List.of(),
                    player.getServer().getTickCount(), 0L, PlayerControlStatus.ACTIVE, 1L);
            var result = lookup.leaseService().orElseThrow().acquire(lease);
            return result.status() == PlayerActorStatus.SUCCEEDED || result.status() == PlayerActorStatus.DUPLICATE
                    ? "" : result.message();
        }

        private void releasePlayer(Lease lease) {
            if (lease == null || lease.player == null) return;
            var lookup = EchoPlayerActorServiceBridge.lookup(lease.player.getServer());
            lookup.leaseService().ifPresent(service -> service.release(PlayerActorId.of(lease.player.getUUID()),
                    playerOwner(lease.rootOwner)));
        }
    }

    private PlayerActorOwnerId playerOwner(String rootOwner) {
        return PlayerActorOwnerId.system("echo_scene_" + SceneCommandHub.token(rootOwner));
    }

    private final class Checkpoints implements SceneCheckpointStore {
        public void save(SceneCheckpoint checkpoint) {
            persistence.save(checkpoint);
            checkpoints.put(checkpoint.executionId(), checkpoint);
        }
        public Optional<SceneCheckpoint> load(SceneExecutionId id) { return Optional.ofNullable(checkpoints.get(id)); }
    }

    private static final class Lease {
        private final String rootOwner;
        private final ServerPlayer player;
        private final LinkedHashSet<SceneExecutionId> owners = new LinkedHashSet<>();
        private Lease(String rootOwner, ServerPlayer player) { this.rootOwner = rootOwner; this.player = player; }
    }
}
