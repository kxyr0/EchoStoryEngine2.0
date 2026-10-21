package io.github.kxyr0.echonavigation.forge.persistence;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class NavigationSavedData extends SavedData {
    private static final String NAME = "echo_navigation_runtime_v1";
    private final List<String> encoded = new ArrayList<>();

    public static NavigationSavedData load(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(NavigationSavedData::load, NavigationSavedData::new, NAME);
    }

    public static NavigationSavedData load(CompoundTag tag) {
        NavigationSavedData data = new NavigationSavedData();
        ListTag list = tag.getList("moves", Tag.TAG_STRING);
        for (int index = 0; index < list.size(); index++) {
            data.encoded.add(list.getString(index));
        }
        return data;
    }

    public void replaceAll(List<String> snapshots) {
        encoded.clear();
        encoded.addAll(snapshots);
        setDirty();
    }

    public List<String> encoded() {
        return List.copyOf(encoded);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        encoded.forEach(value -> list.add(StringTag.valueOf(value)));
        tag.put("moves", list);
        return tag;
    }
}
