package io.github.kxyr0.echocore.features;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class FeatureSavedData extends SavedData {
    private final FeatureState state = new FeatureState();
    public FeatureState state() { return state; }
    public static FeatureSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FeatureSavedData::load, FeatureSavedData::new, "echo_core_features");
    }
    public static FeatureSavedData load(CompoundTag tag) {
        FeatureSavedData data = new FeatureSavedData();
        CompoundTag players = tag.getCompound("players");
        for (String key : players.getAllKeys()) {
            UUID player = UUID.fromString(key);
            CompoundTag flags = players.getCompound(key);
            for (String flag : flags.getAllKeys()) data.state.set(player, flag, flags.getBoolean(flag));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        CompoundTag players = new CompoundTag();
        state.snapshot().forEach((player, flags) -> {
            CompoundTag values = new CompoundTag(); flags.forEach(values::putBoolean);
            players.put(player.toString(), values);
        });
        tag.put("players", players); return tag;
    }
}
