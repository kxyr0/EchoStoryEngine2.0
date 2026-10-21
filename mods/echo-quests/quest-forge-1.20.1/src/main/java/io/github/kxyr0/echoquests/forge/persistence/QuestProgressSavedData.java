package io.github.kxyr0.echoquests.forge.persistence;

import io.github.kxyr0.echoquests.api.identity.QuestId;
import io.github.kxyr0.echoquests.api.identity.QuestStateId;
import io.github.kxyr0.echoquests.api.runtime.QuestProgress;
import io.github.kxyr0.echoquests.api.runtime.QuestStatus;
import io.github.kxyr0.echoquests.core.runtime.QuestProgressKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class QuestProgressSavedData extends SavedData {
    private static final String NAME = "echo_quests_progress";
    private List<QuestProgress> progress;

    private QuestProgressSavedData(List<QuestProgress> progress) { this.progress = List.copyOf(progress); }

    public static QuestProgressSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(QuestProgressSavedData::load,
                () -> new QuestProgressSavedData(List.of()), NAME);
    }

    public Collection<QuestProgress> progress() { return progress; }

    public void capture(Map<QuestProgressKey, QuestProgress> entries) {
        progress = entries.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag values = new ListTag();
        progress.forEach(value -> values.add(write(value)));
        root.put("quests", values);
        return root;
    }

    private static QuestProgressSavedData load(CompoundTag root) {
        List<QuestProgress> values = new ArrayList<>();
        root.getList("quests", Tag.TAG_COMPOUND).forEach(tag -> read((CompoundTag) tag).ifPresent(values::add));
        return new QuestProgressSavedData(values);
    }

    private static CompoundTag write(QuestProgress value) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("player", value.playerId());
        tag.putString("quest", value.questId().value());
        tag.putString("state", value.stateId().value());
        tag.putString("status", value.status().name());
        tag.putLong("revision", value.revision());
        return tag;
    }

    private static java.util.Optional<QuestProgress> read(CompoundTag tag) {
        try {
            if (!tag.hasUUID("player")) return java.util.Optional.empty();
            return java.util.Optional.of(new QuestProgress(tag.getUUID("player"),
                    QuestId.parse(tag.getString("quest")), QuestStateId.parse(tag.getString("state")),
                    QuestStatus.valueOf(tag.getString("status")), tag.getLong("revision")));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }
}
