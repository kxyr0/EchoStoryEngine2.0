package io.github.kxyr0.echoplayer.forge.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * SavedData host for encoded durable player profiles.
 */
public final class PlayerProfileSavedData extends SavedData {
    public static final String NAME = "echo_content_player_profiles";
    private final List<String> encodedProfiles = new ArrayList<>();

    public static PlayerProfileSavedData load(CompoundTag tag) {
        PlayerProfileSavedData data = new PlayerProfileSavedData();
        ListTag list = tag.getList("profiles", Tag.TAG_STRING);
        for (int index = 0; index < list.size(); index++) {
            data.encodedProfiles.add(list.getString(index));
        }
        return data;
    }

    public void replaceAll(List<String> profiles) {
        encodedProfiles.clear();
        encodedProfiles.addAll(profiles);
        setDirty();
    }

    public List<String> encodedProfiles() {
        return List.copyOf(encodedProfiles);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        encodedProfiles.forEach(profile -> list.add(StringTag.valueOf(profile)));
        tag.put("profiles", list);
        return tag;
    }
}
