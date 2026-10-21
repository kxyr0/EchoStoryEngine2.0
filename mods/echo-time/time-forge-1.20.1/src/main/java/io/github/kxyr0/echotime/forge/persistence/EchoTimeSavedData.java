package io.github.kxyr0.echotime.forge.persistence;

import io.github.kxyr0.echotime.core.state.EchoTimeState;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class EchoTimeSavedData extends SavedData {
    private static final String ID = "echo_time_clock";
    private final EchoTimeStateCodec codec = new EchoTimeStateCodec();
    private EchoTimeState state;
    private final boolean newlyCreated;

    private EchoTimeSavedData(EchoTimeState state, boolean newlyCreated) {
        this.state = Objects.requireNonNull(state, "state");
        this.newlyCreated = newlyCreated;
    }

    public static EchoTimeSavedData get(ServerLevel overworld, Supplier<EchoTimeState> initial) {
        Objects.requireNonNull(overworld, "overworld");
        Objects.requireNonNull(initial, "initial");
        EchoTimeStateCodec codec = new EchoTimeStateCodec();
        return overworld.getDataStorage().computeIfAbsent(
                tag -> new EchoTimeSavedData(codec.decode(tag), false),
                () -> new EchoTimeSavedData(initial.get(), true), ID);
    }

    public EchoTimeState state() {
        return state;
    }

    public boolean newlyCreated() {
        return newlyCreated;
    }

    public void update(EchoTimeState next) {
        state = Objects.requireNonNull(next, "next");
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag encoded = codec.encode(state);
        for (String key : encoded.getAllKeys()) tag.put(key, encoded.get(key));
        return tag;
    }
}
