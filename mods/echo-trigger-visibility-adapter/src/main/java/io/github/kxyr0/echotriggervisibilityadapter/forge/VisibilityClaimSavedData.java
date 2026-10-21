package io.github.kxyr0.echotriggervisibilityadapter.forge;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class VisibilityClaimSavedData extends SavedData {
    private static final String FILE_ID = "echo_trigger_visibility_claims";
    private final Map<String, String> terminalEvents = new LinkedHashMap<>();

    public static VisibilityClaimSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                VisibilityClaimSavedData::load, VisibilityClaimSavedData::new, FILE_ID);
    }

    public boolean claim(String eventId) {
        boolean added = terminalEvents.putIfAbsent(eventId, "CLAIMED") == null;
        if (added) {
            setDirty();
        }
        return added;
    }

    public void terminal(String eventId, String result) {
        if (terminalEvents.containsKey(eventId)) {
            terminalEvents.put(eventId, result);
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag values = new ListTag();
        terminalEvents.forEach((event, result) -> {
            CompoundTag value = new CompoundTag();
            value.putString("event", event);
            value.putString("result", result);
            values.add(value);
        });
        tag.put("terminalEvents", values);
        return tag;
    }

    private static VisibilityClaimSavedData load(CompoundTag tag) {
        VisibilityClaimSavedData data = new VisibilityClaimSavedData();
        ListTag values = tag.getList("terminalEvents", Tag.TAG_COMPOUND);
        for (int index = 0; index < values.size(); index++) {
            CompoundTag value = values.getCompound(index);
            data.terminalEvents.put(value.getString("event"), value.getString("result"));
        }
        return data;
    }
}
