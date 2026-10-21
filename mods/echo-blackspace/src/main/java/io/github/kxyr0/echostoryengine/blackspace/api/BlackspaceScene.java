package io.github.kxyr0.echostoryengine.blackspace.api;

import io.github.kxyr0.echostoryengine.blackspace.EchoBlackspace;
import io.github.kxyr0.echostoryengine.blackspace.network.BlackspaceNetwork;
import io.github.kxyr0.echostoryengine.blackspace.recovery.BlackspaceLifecycleState;
import io.github.kxyr0.echostoryengine.blackspace.recovery.BlackspaceRecoverySavedData;
import io.github.kxyr0.echostoryengine.blackspace.recovery.BlackspaceRecoverySnapshot;
import io.github.kxyr0.echostoryengine.blackspace.world.BlackspaceInstances;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;

public final class BlackspaceScene {
    private static final Map<UUID, BlackspaceLifecycleState> SESSION_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingTeleport> PENDING_TELEPORTS = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingClientReset> PENDING_CLIENT_RESETS = new ConcurrentHashMap<>();
    private static final Set<UUID> MOVEMENT_LOCKS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> WARNED_UNMANAGED_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final AtomicLong NEXT_PREPARE_ID = new AtomicLong();
    private static final AtomicLong NEXT_RESET_ID = new AtomicLong();
    private static final int PREPARE_TIMEOUT_TICKS = 20 * 20;
    private static final int CLIENT_RESET_TIMEOUT_TICKS = 20 * 10;
    private static final BlackspaceInstances INSTANCES = new BlackspaceInstances();

    private BlackspaceScene() {
    }

    public static BlackspaceInstance create(ServerPlayer player, String instanceId) {
        Objects.requireNonNull(player, "player");
        BlackspaceInstance instance = INSTANCES.getOrCreate(player, instanceId);
        EchoBlackspace.LOGGER.info("Blackspace {} ready at {} {} {} in {}.",
                instance.id(),
                instance.spawnX(),
                instance.spawnY(),
                instance.spawnZ(),
                instance.dimensionId());
        return instance;
    }

    public static Optional<BlackspaceInstance> find(ServerPlayer player, String instanceId) {
        Objects.requireNonNull(player, "player");
        return INSTANCES.find(player, instanceId);
    }

    public static void teleportTo(ServerPlayer player, BlackspaceInstance instance) {
        enter(player, instance, () -> {
        });
    }

    public static void enter(ServerPlayer player, BlackspaceInstance instance, Runnable completion) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(instance, "instance");
        Runnable callback = completion == null ? () -> {
        } : completion;
        UUID playerId = player.getUUID();
        if (SESSION_STATES.get(playerId) == BlackspaceLifecycleState.ACTIVE && instance.contains(player)) {
            runCallback(callback, "Blackspace enter callback");
            return;
        }
        PendingTeleport existing = PENDING_TELEPORTS.get(playerId);
        if (existing != null && existing.instance().id().equals(instance.id())) {
            PENDING_TELEPORTS.put(playerId, existing.withCompletion(callback));
            return;
        }
        MinecraftServer server = requireServer(player);
        ServerLevel level = requireLevel(server, instance.dimension());
        ensureRecoverySnapshot(player, instance.id(), BlackspaceLifecycleState.ENTERING);
        SESSION_STATES.put(playerId, BlackspaceLifecycleState.ENTERING);
        level.getChunk(BlockPos.containing(instance.spawnX(), instance.spawnY(), instance.spawnZ()));
        long prepareId = NEXT_PREPARE_ID.incrementAndGet();
        PENDING_TELEPORTS.put(playerId, new PendingTeleport(instance, prepareId, server.getTickCount(), callback));
        if (!BlackspaceNetwork.sendPrepareEnter(player, prepareId)) {
            completePreparedTeleport(player, prepareId);
        }
    }

    public static void completePreparedTeleport(ServerPlayer player, long prepareId) {
        Objects.requireNonNull(player, "player");
        PendingTeleport pending = PENDING_TELEPORTS.get(player.getUUID());
        if (pending == null || pending.prepareId() != prepareId) {
            return;
        }
        PENDING_TELEPORTS.remove(player.getUUID(), pending);
        teleportPrepared(player, pending.instance(), pending.completion());
    }

    public static void completeClientReset(ServerPlayer player, long resetId) {
        Objects.requireNonNull(player, "player");
        PendingClientReset pending = PENDING_CLIENT_RESETS.get(player.getUUID());
        if (pending == null || pending.resetId() != resetId) {
            return;
        }
        PENDING_CLIENT_RESETS.remove(player.getUUID(), pending);
        MOVEMENT_LOCKS.remove(player.getUUID());
        SESSION_STATES.remove(player.getUUID());
        BlackspaceRecoverySavedData.open(requireServer(player)).remove(player.getUUID());
        EchoBlackspace.LOGGER.info("Blackspace client reset confirmed for {}.",
                player.getGameProfile().getName());
        runCallback(pending.completion(), "Blackspace exit callback");
    }

    public static void start(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        BlackspaceRecoverySavedData data = BlackspaceRecoverySavedData.open(requireServer(player));
        String blackspaceId = data.find(player.getUUID())
                .map(BlackspaceRecoverySnapshot::blackspaceId)
                .or(() -> INSTANCES.containing(player).map(BlackspaceInstance::id))
                .orElse("echo_blackspace:unknown");
        data.find(player.getUUID())
                .ifPresent(snapshot -> data.put(snapshot.withBlackspaceIdAndState(blackspaceId,
                        BlackspaceLifecycleState.ACTIVE)));
        SESSION_STATES.put(player.getUUID(), BlackspaceLifecycleState.ACTIVE);
        BlackspaceNetwork.sendSessionState(player, true, blackspaceId, 0L);
        EchoBlackspace.LOGGER.info("Blackspace enabled for {}.", player.getGameProfile().getName());
    }

    public static void stop(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        exit(player, () -> {
        });
    }

    public static boolean exit(ServerPlayer player, Runnable completion) {
        Objects.requireNonNull(player, "player");
        return beginExit(player, "normal stop", completion == null ? () -> {
        } : completion);
    }

    public static boolean isActive(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        BlackspaceLifecycleState state = SESSION_STATES.get(player.getUUID());
        return state != null && state.requiresRecovery();
    }

    public static void onPlayerLoggedIn(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        MinecraftServer server = requireServer(player);
        BlackspaceRecoverySavedData data = BlackspaceRecoverySavedData.open(server);
        Optional<BlackspaceRecoverySnapshot> snapshot = data.find(player.getUUID());
        if (snapshot.isEmpty() || !snapshot.orElseThrow().state().requiresRecovery()) {
            SESSION_STATES.remove(player.getUUID());
            BlackspaceNetwork.sendSessionState(player, false, "", 0L);
            return;
        }
        BlackspaceRecoverySnapshot recovery = snapshot.orElseThrow().withState(
                BlackspaceLifecycleState.RECOVERY_REQUIRED);
        data.put(recovery);
        SESSION_STATES.put(player.getUUID(), BlackspaceLifecycleState.RECOVERY_REQUIRED);
        MOVEMENT_LOCKS.add(player.getUUID());
        restorePlayer(player, recovery, "login recovery");
        sendResetAndTrack(player, recovery.blackspaceId(), "login recovery");
        EchoBlackspace.LOGGER.warn("Recovering {} from interrupted Blackspace session {}.",
                player.getGameProfile().getName(), recovery.blackspaceId());
    }

    public static void onPlayerLoggedOut(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        UUID playerId = player.getUUID();
        PENDING_TELEPORTS.remove(playerId);
        PENDING_CLIENT_RESETS.remove(playerId);
        MOVEMENT_LOCKS.remove(playerId);
        WARNED_UNMANAGED_PLAYERS.remove(playerId);
        BlackspaceRecoverySavedData data = BlackspaceRecoverySavedData.open(requireServer(player));
        data.find(playerId)
                .filter(snapshot -> snapshot.state().requiresRecovery())
                .ifPresent(snapshot -> data.put(snapshot.withState(BlackspaceLifecycleState.RECOVERY_REQUIRED)));
        SESSION_STATES.remove(playerId);
    }

    public static void discard(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        UUID playerId = player.getUUID();
        PENDING_TELEPORTS.remove(playerId);
        PENDING_CLIENT_RESETS.remove(playerId);
        MOVEMENT_LOCKS.remove(playerId);
        WARNED_UNMANAGED_PLAYERS.remove(playerId);
        SESSION_STATES.remove(playerId);
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        expirePendingTeleports(server);
        expirePendingClientResets(server);
        freezeLockedPlayers(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BlackspaceLifecycleState state = SESSION_STATES.get(player.getUUID());
            if (state == BlackspaceLifecycleState.ACTIVE && INSTANCES.containing(player).isEmpty()) {
                beginExit(player, "left Blackspace bounds", () -> {
                });
            }
            warnIfUnmanagedBlackspaceTeleport(player, state);
        }
    }

    public static void onServerStopping(MinecraftServer server) {
        BlackspaceRecoverySavedData data = BlackspaceRecoverySavedData.open(server);
        for (BlackspaceRecoverySnapshot snapshot : data.all()) {
            if (snapshot.state().requiresRecovery()) {
                data.put(snapshot.withState(BlackspaceLifecycleState.RECOVERY_REQUIRED));
            }
        }
        clearRuntime();
    }

    public static void clearRuntime() {
        SESSION_STATES.clear();
        PENDING_TELEPORTS.clear();
        PENDING_CLIENT_RESETS.clear();
        MOVEMENT_LOCKS.clear();
        WARNED_UNMANAGED_PLAYERS.clear();
        INSTANCES.clear();
    }

    public static boolean isProtectedPlatformBlock(ServerLevel level, BlockPos pos) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        return INSTANCES.isPlatformBlock(level, pos);
    }

    private static void ensureRecoverySnapshot(ServerPlayer player, String blackspaceId,
            BlackspaceLifecycleState state) {
        MinecraftServer server = requireServer(player);
        BlackspaceRecoverySavedData data = BlackspaceRecoverySavedData.open(server);
        Optional<BlackspaceRecoverySnapshot> existing = data.find(player.getUUID());
        if (existing.isPresent() && existing.orElseThrow().state().requiresRecovery()) {
            data.put(existing.orElseThrow().withBlackspaceIdAndState(blackspaceId, state));
            return;
        }
        ResourceLocation dimension = player.level().dimension().location();
        data.put(new BlackspaceRecoverySnapshot(
                player.getUUID(),
                blackspaceId,
                dimension,
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                state,
                server.getTickCount()
        ));
    }

    private static void teleportPrepared(ServerPlayer player, BlackspaceInstance instance) {
        teleportPrepared(player, instance, () -> {
        });
    }

    private static void teleportPrepared(ServerPlayer player, BlackspaceInstance instance, Runnable completion) {
        MinecraftServer server = requireServer(player);
        ServerLevel level = requireLevel(server, instance.dimension());
        level.getChunk(BlockPos.containing(instance.spawnX(), instance.spawnY(), instance.spawnZ()));
        player.teleportTo(level, instance.spawnX(), instance.spawnY(), instance.spawnZ(), Set.<RelativeMovement>of(),
                instance.yaw(), instance.pitch());
        player.fallDistance = 0.0F;
        BlackspaceRecoverySavedData.open(server).updateState(player.getUUID(), BlackspaceLifecycleState.ACTIVE);
        SESSION_STATES.put(player.getUUID(), BlackspaceLifecycleState.ACTIVE);
        BlackspaceNetwork.sendSessionState(player, true, instance.id(), 0L);
        EchoBlackspace.LOGGER.info("Blackspace enabled for {}.", player.getGameProfile().getName());
        runCallback(completion, "Blackspace enter callback");
    }

    private static boolean beginExit(ServerPlayer player, String reason, Runnable completion) {
        MinecraftServer server = requireServer(player);
        UUID playerId = player.getUUID();
        PENDING_TELEPORTS.remove(playerId);
        PendingClientReset existingReset = PENDING_CLIENT_RESETS.get(playerId);
        if (existingReset != null) {
            PENDING_CLIENT_RESETS.put(playerId, existingReset.withCompletion(completion));
            return true;
        }
        BlackspaceRecoverySavedData data = BlackspaceRecoverySavedData.open(server);
        Optional<BlackspaceRecoverySnapshot> snapshot = data.find(playerId);
        if (snapshot.isEmpty() && SESSION_STATES.get(playerId) == null) {
            runCallback(completion, "Blackspace exit callback");
            return false;
        }
        SESSION_STATES.put(playerId, BlackspaceLifecycleState.EXITING);
        MOVEMENT_LOCKS.add(playerId);
        if (snapshot.isPresent()) {
            BlackspaceRecoverySnapshot exiting = snapshot.orElseThrow().withState(BlackspaceLifecycleState.EXITING);
            data.put(exiting);
            sendResetAndTrack(player, exiting.blackspaceId(), reason, completion);
            restorePlayer(player, exiting, reason);
        } else {
            EchoBlackspace.LOGGER.warn("Blackspace stop for {} had no recovery snapshot; using client reset only.",
                    player.getGameProfile().getName());
            sendResetAndTrack(player, "", reason, completion);
        }
        EchoBlackspace.LOGGER.info("Blackspace disabled for {}.", player.getGameProfile().getName());
        return true;
    }

    private static void restorePlayer(ServerPlayer player, BlackspaceRecoverySnapshot snapshot, String reason) {
        MinecraftServer server = requireServer(player);
        ServerLevel target = server.getLevel(ResourceKey.create(Registries.DIMENSION, snapshot.originDimension()));
        double x = snapshot.originX();
        double y = snapshot.originY();
        double z = snapshot.originZ();
        float yaw = snapshot.yaw();
        float pitch = snapshot.pitch();
        if (target == null || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            target = server.overworld();
            BlockPos spawn = target.getSharedSpawnPos();
            x = spawn.getX() + 0.5D;
            y = spawn.getY();
            z = spawn.getZ() + 0.5D;
            yaw = target.getSharedSpawnAngle();
            pitch = 0.0F;
            EchoBlackspace.LOGGER.warn("Blackspace {} for {} could not use saved return point {}; using overworld spawn.",
                    reason, player.getGameProfile().getName(), snapshot.originDimension());
        }
        target.getChunk(BlockPos.containing(x, y, z));
        player.teleportTo(target, x, y, z, Set.<RelativeMovement>of(), yaw, pitch);
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;
        EchoBlackspace.LOGGER.info("Blackspace {} restored {} to {} {} {} in {}.",
                reason, player.getGameProfile().getName(), x, y, z, target.dimension().location());
    }

    private static void sendResetAndTrack(ServerPlayer player, String blackspaceId, String reason) {
        sendResetAndTrack(player, blackspaceId, reason, () -> {
        });
    }

    private static void sendResetAndTrack(
            ServerPlayer player,
            String blackspaceId,
            String reason,
            Runnable completion) {
        long resetId = NEXT_RESET_ID.incrementAndGet();
        PENDING_CLIENT_RESETS.put(player.getUUID(), new PendingClientReset(resetId,
                requireServer(player).getTickCount(), reason, completion));
        if (!BlackspaceNetwork.sendSessionState(player, false, blackspaceId, resetId)) {
            EchoBlackspace.LOGGER.warn("Blackspace client reset packet failed for {}; waiting for timeout.",
                    player.getGameProfile().getName());
        }
    }

    private static void expirePendingTeleports(MinecraftServer server) {
        int now = server.getTickCount();
        Iterator<Map.Entry<UUID, PendingTeleport>> iterator = PENDING_TELEPORTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingTeleport> entry = iterator.next();
            PendingTeleport pending = entry.getValue();
            if (now - pending.createdAtTick() < PREPARE_TIMEOUT_TICKS) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            iterator.remove();
            if (player != null) {
                EchoBlackspace.LOGGER.warn("Blackspace shader prepare timed out for {}; teleporting with fallback.",
                        player.getGameProfile().getName());
                teleportPrepared(player, pending.instance(), pending.completion());
            } else {
                BlackspaceRecoverySavedData.open(server).updateState(entry.getKey(),
                        BlackspaceLifecycleState.RECOVERY_REQUIRED);
            }
        }
    }

    private static void expirePendingClientResets(MinecraftServer server) {
        int now = server.getTickCount();
        BlackspaceRecoverySavedData data = BlackspaceRecoverySavedData.open(server);
        Iterator<Map.Entry<UUID, PendingClientReset>> iterator = PENDING_CLIENT_RESETS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingClientReset> entry = iterator.next();
            PendingClientReset pending = entry.getValue();
            if (now - pending.createdAtTick() < CLIENT_RESET_TIMEOUT_TICKS) {
                continue;
            }
            iterator.remove();
            UUID playerId = entry.getKey();
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                data.updateState(playerId, BlackspaceLifecycleState.RECOVERY_REQUIRED);
                continue;
            }
            MOVEMENT_LOCKS.remove(playerId);
            SESSION_STATES.remove(playerId);
            data.remove(playerId);
            EchoBlackspace.LOGGER.warn("Blackspace client reset timed out for {}; server-side return was already applied.",
                    player.getGameProfile().getName());
            runCallback(pending.completion(), "Blackspace exit timeout callback");
        }
    }

    private static void warnIfUnmanagedBlackspaceTeleport(ServerPlayer player, BlackspaceLifecycleState state) {
        Optional<BlackspaceInstance> containing = INSTANCES.containing(player);
        if (containing.isEmpty() || state != null && state.requiresRecovery()) {
            WARNED_UNMANAGED_PLAYERS.remove(player.getUUID());
            return;
        }
        if (WARNED_UNMANAGED_PLAYERS.add(player.getUUID())) {
            EchoBlackspace.LOGGER.warn(
                    "Player {} is inside Blackspace {} without an active Blackspace session. "
                            + "Use Sleep.EnterDream(blackspace) or Player.Teleport(blackspace), not a raw BlockPos.",
                    player.getGameProfile().getName(), containing.orElseThrow().id());
        }
    }

    private static void runCallback(Runnable callback, String label) {
        try {
            callback.run();
        } catch (RuntimeException exception) {
            EchoBlackspace.LOGGER.warn("{} failed.", label, exception);
        }
    }

    private static void freezeLockedPlayers(MinecraftServer server) {
        for (UUID playerId : MOVEMENT_LOCKS) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                player.fallDistance = 0.0F;
            }
        }
    }

    private static MinecraftServer requireServer(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            throw new IllegalStateException("Cannot use Blackspace without an active server.");
        }
        return server;
    }

    private static ServerLevel requireLevel(MinecraftServer server, ResourceKey<Level> dimension) {
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            throw new IllegalStateException("Blackspace dimension is not loaded: " + dimension.location());
        }
        return level;
    }

    private record PendingTeleport(
            BlackspaceInstance instance,
            long prepareId,
            int createdAtTick,
            Runnable completion) {
        PendingTeleport withCompletion(Runnable nextCompletion) {
            return new PendingTeleport(instance, prepareId, createdAtTick,
                    () -> {
                        runCallback(completion, "Blackspace enter callback");
                        runCallback(nextCompletion, "Blackspace enter callback");
                    });
        }
    }

    private record PendingClientReset(long resetId, int createdAtTick, String reason, Runnable completion) {
        PendingClientReset withCompletion(Runnable nextCompletion) {
            return new PendingClientReset(resetId, createdAtTick, reason,
                    () -> {
                        runCallback(completion, "Blackspace exit callback");
                        runCallback(nextCompletion, "Blackspace exit callback");
                    });
        }
    }
}
