package io.github.kxyr0.echobackgroundnpc.forge.persistence;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.runtime.AgentLifeState;
import io.github.kxyr0.echobackgroundnpc.core.simulation.BackgroundAgentRuntime;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Saves durable exceptions only; schedule/route state is reconstructed from Echo Time. */
public final class BackgroundNpcSavedData extends SavedData {
    private static final String NAME = "echo_background_npc";
    private final Set<String> dead = new LinkedHashSet<>();
    private final Set<String> occurrences = new LinkedHashSet<>();

    public static BackgroundNpcSavedData load(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BackgroundNpcSavedData::read,
                BackgroundNpcSavedData::new, NAME);
    }

    public void restore(BackgroundAgentRuntime runtime) {
        dead.forEach(value -> runtime.markDead(new BackgroundNpcId(value)));
        runtime.occurrences().restore(occurrences);
    }

    public void capture(BackgroundAgentRuntime runtime) {
        dead.clear();
        runtime.states().stream().filter(value -> value.life() == AgentLifeState.DEAD)
                .map(value -> value.id().value()).forEach(dead::add);
        occurrences.clear();
        occurrences.addAll(runtime.occurrences().snapshot());
        setDirty();
    }

    @Override public CompoundTag save(CompoundTag tag) {
        tag.putInt("Schema", 1);
        tag.put("Dead", strings(dead));
        tag.put("Occurrences", strings(occurrences));
        return tag;
    }

    private static BackgroundNpcSavedData read(CompoundTag tag) {
        BackgroundNpcSavedData data = new BackgroundNpcSavedData();
        readStrings(tag.getList("Dead", Tag.TAG_STRING), data.dead);
        readStrings(tag.getList("Occurrences", Tag.TAG_STRING), data.occurrences);
        return data;
    }
    private static ListTag strings(Set<String> values) {
        ListTag result = new ListTag();
        values.forEach(value -> result.add(StringTag.valueOf(value)));
        return result;
    }
    private static void readStrings(ListTag values, Set<String> target) {
        for (int index = 0; index < values.size(); index++) target.add(values.getString(index));
    }
}
