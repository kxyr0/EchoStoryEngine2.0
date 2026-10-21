package io.github.kxyr0.echoplayersleep.forge.service;

import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class ForgePlayerSleepCountSavedData extends SavedData {
    private static final String DATA_NAME = "echo_player_sleep_counts";
    private static final String COUNTS = "counts";
    private static final String PLAYER = "player";
    private static final String COUNT = "count";
    private final Map<UUID, Long> counts = new LinkedHashMap<>();

    public static ForgePlayerSleepCountSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                ForgePlayerSleepCountSavedData::load,
                ForgePlayerSleepCountSavedData::new,
                DATA_NAME);
    }

    public static ForgePlayerSleepCountSavedData load(CompoundTag tag) {
        ForgePlayerSleepCountSavedData data = new ForgePlayerSleepCountSavedData();
        ListTag entries = tag.getList(COUNTS, Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            if (entry.hasUUID(PLAYER) && entry.contains(COUNT, Tag.TAG_LONG)) {
                long count = entry.getLong(COUNT);
                if (count > 0L) {
                    data.counts.put(entry.getUUID(PLAYER), count);
                }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        counts.forEach((player, count) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID(PLAYER, player);
            entry.putLong(COUNT, count);
            entries.add(entry);
        });
        tag.put(COUNTS, entries);
        return tag;
    }

    public long record(PlayerRef player) {
        long next = counts.getOrDefault(player.uuid(), 0L) + 1L;
        counts.put(player.uuid(), next);
        setDirty();
        return next;
    }

    public long count(PlayerRef player) {
        return counts.getOrDefault(player.uuid(), 0L);
    }

    public long reset(PlayerRef player) {
        Long previous = counts.remove(player.uuid());
        if (previous != null) {
            setDirty();
            return previous;
        }
        return 0L;
    }
}
