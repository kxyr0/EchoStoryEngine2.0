package io.github.kxyr0.echocutscene.forge.playback.persistence;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class CutscenePlaybackSavedData extends SavedData {
    static final String NAME = "echo_cutscene_playbacks_v1";
    private final Map<UUID, SavedCutscenePlayback> playbacks = new LinkedHashMap<>();

    public static CutscenePlaybackSavedData open(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                CutscenePlaybackSavedData::load, CutscenePlaybackSavedData::new, NAME);
    }

    public static CutscenePlaybackSavedData load(CompoundTag tag) {
        CutscenePlaybackSavedData data = new CutscenePlaybackSavedData();
        new CutscenePlaybackNbtCodec().read(tag, data.playbacks);
        return data;
    }

    public Optional<SavedCutscenePlayback> find(UUID playerId) {
        return Optional.ofNullable(playbacks.get(playerId));
    }

    public void put(SavedCutscenePlayback playback) {
        playbacks.put(playback.playerId(), playback);
        setDirty();
    }

    public void remove(UUID playerId) {
        if (playbacks.remove(playerId) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        return new CutscenePlaybackNbtCodec().write(playbacks, tag);
    }
}
