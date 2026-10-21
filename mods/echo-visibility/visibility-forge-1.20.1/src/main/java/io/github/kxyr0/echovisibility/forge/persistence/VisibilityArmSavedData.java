package io.github.kxyr0.echovisibility.forge.persistence;

import io.github.kxyr0.echovisibility.api.identity.VisibilityPlayerRef;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import io.github.kxyr0.echovisibility.core.arm.VisibilityArmSnapshot;
import io.github.kxyr0.echovisibility.core.arm.VisibilityArmRecovery;
import io.github.kxyr0.echovisibility.core.arm.VisibilityGenerationSnapshot;
import io.github.kxyr0.echovisibility.forge.network.codec.VisibilityDefinitionCodec;
import io.github.kxyr0.echovisibility.forge.service.VisibilityServerState;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class VisibilityArmSavedData extends SavedData {
    private static final String FILE_ID = "echo_visibility_arms";
    private final VisibilityServerState state;

    private VisibilityArmSavedData(VisibilityServerState state) {
        this.state = state;
    }

    public static VisibilityArmSavedData get(
            MinecraftServer server,
            VisibilityServerState state) {
        VisibilityArmSavedData data = server.overworld().getDataStorage().computeIfAbsent(
                tag -> load(tag, state), () -> new VisibilityArmSavedData(state), FILE_ID);
        state.persistenceDirty(data::setDirty);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag arms = new ListTag();
        for (VisibilityArmSnapshot snapshot : state.arms().all()) {
            arms.add(write(snapshot));
        }
        tag.put("arms", arms);
        ListTag generations = new ListTag();
        for (VisibilityGenerationSnapshot snapshot : state.arms().generations()) {
            CompoundTag value = new CompoundTag();
            value.putUUID("player", snapshot.player().uuid());
            value.putString("watch", snapshot.watchId().value());
            value.putLong("generation", snapshot.generation().value());
            generations.add(value);
        }
        tag.put("generations", generations);
        return tag;
    }

    private static VisibilityArmSavedData load(CompoundTag tag, VisibilityServerState state) {
        VisibilityArmSavedData data = new VisibilityArmSavedData(state);
        ListTag generations = tag.getList("generations", Tag.TAG_COMPOUND);
        for (int index = 0; index < generations.size(); index++) {
            CompoundTag value = generations.getCompound(index);
            state.arms().restoreGeneration(new VisibilityGenerationSnapshot(
                    new VisibilityPlayerRef(value.getUUID("player")),
                    new io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId(
                            value.getString("watch")),
                    new VisibilityWatchGeneration(value.getLong("generation"))));
        }
        ListTag arms = tag.getList("arms", Tag.TAG_COMPOUND);
        for (int index = 0; index < arms.size(); index++) {
            try {
                restore(arms.getCompound(index), state);
            } catch (RuntimeException ignored) {
                // One corrupt persisted arm must not block unrelated watch recovery.
            }
        }
        return data;
    }

    private static CompoundTag write(VisibilityArmSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("player", snapshot.player().uuid());
        tag.putByteArray("definition", encode(snapshot.definition()));
        tag.putLong("generation", snapshot.generation().value());
        tag.putString("ownerLineage", snapshot.ownerLineage());
        tag.putLong("createdTick", snapshot.createdTick());
        tag.putLong("connectionGeneration", snapshot.connectionGeneration());
        tag.putString("status", snapshot.status().name());
        tag.putBoolean("requireVisible", snapshot.definition().requireVisibleFirst());
        tag.putLong("lastSequence", snapshot.lastAcceptedSequence());
        tag.putString("lastState", snapshot.lastState().name());
        snapshot.lastTransition().ifPresent(value -> {
            CompoundTag transition = new CompoundTag();
            transition.putString("kind", value.kind().name());
            transition.putString("previous", value.previous().name());
            transition.putString("current", value.current().name());
            transition.putLong("previousFrame", value.previousStableFrame());
            transition.putLong("renderedFrame", value.renderedFrame());
            tag.put("lastTransition", transition);
        });
        return tag;
    }

    private static void restore(CompoundTag tag, VisibilityServerState state) {
        VisibilityWatchDefinition definition = decode(tag.getByteArray("definition"));
        state.arms().restore(new VisibilityArmRecovery(new VisibilityPlayerRef(tag.getUUID("player")), definition,
                new VisibilityWatchGeneration(tag.getLong("generation")),
                tag.getString("ownerLineage"), Math.max(0L, tag.getLong("createdTick")),
                Math.max(1L, tag.getLong("connectionGeneration"))));
    }

    private static byte[] encode(VisibilityWatchDefinition definition) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            VisibilityDefinitionCodec.write(buffer, definition);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.getBytes(buffer.readerIndex(), bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }

    private static VisibilityWatchDefinition decode(byte[] bytes) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {
            return VisibilityDefinitionCodec.read(buffer);
        } finally {
            buffer.release();
        }
    }
}
