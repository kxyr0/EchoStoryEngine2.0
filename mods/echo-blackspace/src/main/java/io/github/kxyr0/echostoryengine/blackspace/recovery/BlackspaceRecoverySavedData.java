package io.github.kxyr0.echostoryengine.blackspace.recovery;

import io.github.kxyr0.echostoryengine.blackspace.EchoBlackspace;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class BlackspaceRecoverySavedData extends SavedData {
    private static final String NAME = "echo_blackspace_recovery_v1";
    private static final String SNAPSHOTS = "snapshots";
    private static final String PLAYER = "player";
    private static final String BLACKSPACE_ID = "blackspaceId";
    private static final String DIMENSION = "dimension";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";
    private static final String YAW = "yaw";
    private static final String PITCH = "pitch";
    private static final String STATE = "state";
    private static final String CREATED_AT_TICK = "createdAtTick";

    private final Map<UUID, BlackspaceRecoverySnapshot> snapshots = new LinkedHashMap<>();

    public static BlackspaceRecoverySavedData open(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                BlackspaceRecoverySavedData::load,
                BlackspaceRecoverySavedData::new,
                NAME);
    }

    public static BlackspaceRecoverySavedData load(CompoundTag tag) {
        BlackspaceRecoverySavedData data = new BlackspaceRecoverySavedData();
        ListTag entries = tag.getList(SNAPSHOTS, Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            readSnapshot(entries.getCompound(index)).ifPresent(snapshot -> data.snapshots.put(
                    snapshot.playerId(), snapshot));
        }
        return data;
    }

    public Optional<BlackspaceRecoverySnapshot> find(UUID playerId) {
        return Optional.ofNullable(snapshots.get(playerId));
    }

    public void put(BlackspaceRecoverySnapshot snapshot) {
        snapshots.put(snapshot.playerId(), snapshot);
        setDirty();
    }

    public void updateState(UUID playerId, BlackspaceLifecycleState state) {
        BlackspaceRecoverySnapshot existing = snapshots.get(playerId);
        if (existing != null && existing.state() != state) {
            snapshots.put(playerId, existing.withState(state));
            setDirty();
        }
    }

    public void remove(UUID playerId) {
        if (snapshots.remove(playerId) != null) {
            setDirty();
        }
    }

    public Iterable<BlackspaceRecoverySnapshot> all() {
        return List.copyOf(snapshots.values());
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        snapshots.values().forEach(snapshot -> entries.add(writeSnapshot(snapshot)));
        tag.put(SNAPSHOTS, entries);
        return tag;
    }

    private static CompoundTag writeSnapshot(BlackspaceRecoverySnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(PLAYER, snapshot.playerId());
        tag.putString(BLACKSPACE_ID, snapshot.blackspaceId());
        tag.putString(DIMENSION, snapshot.originDimension().toString());
        tag.putDouble(X, snapshot.originX());
        tag.putDouble(Y, snapshot.originY());
        tag.putDouble(Z, snapshot.originZ());
        tag.putFloat(YAW, snapshot.yaw());
        tag.putFloat(PITCH, snapshot.pitch());
        tag.putString(STATE, snapshot.state().name());
        tag.putLong(CREATED_AT_TICK, snapshot.createdAtTick());
        return tag;
    }

    private static Optional<BlackspaceRecoverySnapshot> readSnapshot(CompoundTag tag) {
        try {
            if (!tag.hasUUID(PLAYER)) {
                return Optional.empty();
            }
            ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(DIMENSION));
            if (dimension == null) {
                EchoBlackspace.LOGGER.warn("Skipping Blackspace recovery snapshot with invalid dimension '{}'.",
                        tag.getString(DIMENSION));
                return Optional.empty();
            }
            return Optional.of(new BlackspaceRecoverySnapshot(
                    tag.getUUID(PLAYER),
                    tag.getString(BLACKSPACE_ID),
                    dimension,
                    tag.getDouble(X),
                    tag.getDouble(Y),
                    tag.getDouble(Z),
                    tag.getFloat(YAW),
                    tag.getFloat(PITCH),
                    BlackspaceLifecycleState.parse(tag.getString(STATE)),
                    tag.getLong(CREATED_AT_TICK)
            ));
        } catch (RuntimeException exception) {
            EchoBlackspace.LOGGER.warn("Skipping malformed Blackspace recovery snapshot.", exception);
            return Optional.empty();
        }
    }
}
