package io.github.kxyr0.echomagic.forge.persistence;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureSymbol;
import io.github.kxyr0.echomagic.api.gesture.MagicGestureTemplateTarget;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import io.github.kxyr0.echomagic.core.gesture.template.MagicGestureTemplateLibrary;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class MagicPlayerSavedData extends SavedData {
    private static final String DATA_NAME = "echo_magic_players";
    private final Map<UUID, PlayerMagicPersistence> players = new HashMap<>();

    public static MagicPlayerSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(MagicPlayerSavedData::load,
                MagicPlayerSavedData::new, DATA_NAME);
    }

    public synchronized PlayerMagicPersistence player(UUID player) {
        return players.getOrDefault(player, PlayerMagicPersistence.empty());
    }

    public synchronized void put(UUID player, PlayerMagicPersistence value) {
        players.put(player, value);
        setDirty();
    }

    @Override public synchronized CompoundTag save(CompoundTag root) {
        ListTag list = new ListTag();
        players.forEach((uuid, value) -> list.add(writePlayer(uuid, value)));
        root.put("players", list);
        return root;
    }

    private CompoundTag writePlayer(UUID uuid, PlayerMagicPersistence value) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", uuid);
        ListTag known = new ListTag();
        value.known().stream().sorted().forEach(id -> known.add(StringTag.valueOf(id.value())));
        tag.put("known", known);
        value.selected().ifPresent(id -> tag.putString("selected", id.value()));
        ListTag cooldowns = new ListTag();
        value.cooldownRemainingTicks().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            CompoundTag cooldown = new CompoundTag();
            cooldown.putString("spell", entry.getKey().value());
            cooldown.putLong("remaining", entry.getValue());
            cooldowns.add(cooldown);
        });
        tag.put("cooldowns", cooldowns);
        value.gestureSchool().ifPresent(school -> tag.putString("gestureSchool", school.serializedName()));
        tag.put("gestureTemplates", writeTemplates(value.gestureTemplates()));
        tag.putLong("knownRevision", value.knownRevision());
        tag.putLong("selectedRevision", value.selectedRevision());
        return tag;
    }

    private static MagicPlayerSavedData load(CompoundTag root) {
        MagicPlayerSavedData data = new MagicPlayerSavedData();
        ListTag list = root.getList("players", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag player = list.getCompound(index);
            if (player.hasUUID("uuid")) data.players.put(player.getUUID("uuid"), readPlayer(player));
        }
        return data;
    }

    private static PlayerMagicPersistence readPlayer(CompoundTag tag) {
        Set<SpellId> known = new LinkedHashSet<>();
        ListTag knownTags = tag.getList("known", Tag.TAG_STRING);
        for (int index = 0; index < knownTags.size(); index++) known.add(new SpellId(knownTags.getString(index)));
        Optional<SpellId> selected = tag.contains("selected", Tag.TAG_STRING)
                ? Optional.of(new SpellId(tag.getString("selected"))) : Optional.empty();
        Map<SpellId, Long> cooldowns = new HashMap<>();
        ListTag cooldownTags = tag.getList("cooldowns", Tag.TAG_COMPOUND);
        for (int index = 0; index < cooldownTags.size(); index++) {
            CompoundTag cooldown = cooldownTags.getCompound(index);
            if (cooldown.contains("remaining", Tag.TAG_LONG)) {
                cooldowns.put(new SpellId(cooldown.getString("spell")), cooldown.getLong("remaining"));
            }
        }
        Optional<MagicGestureSymbol> gestureSchool = tag.contains("gestureSchool", Tag.TAG_STRING)
                ? MagicGestureSymbol.fromSerialized(tag.getString("gestureSchool")) : Optional.empty();
        return new PlayerMagicPersistence(known, selected, cooldowns, gestureSchool,
                readTemplates(tag.getList("gestureTemplates", Tag.TAG_COMPOUND)), tag.getLong("knownRevision"),
                tag.getLong("selectedRevision"));
    }

    private static ListTag writeTemplates(
            Map<MagicGestureTemplateTarget, MagicGestureTemplateLibrary.Template> templates) {
        ListTag result = new ListTag();
        templates.values().stream().sorted(java.util.Comparator.comparing(value -> value.target().ordinal()))
                .forEach(template -> {
                    CompoundTag tag = new CompoundTag();
                    tag.putString("target", template.target().serializedName());
                    tag.putDouble("threshold", template.threshold());
                    tag.put("points", writePoints(template.points()));
                    ListTag variants = new ListTag();
                    template.variants().forEach(variant -> {
                        CompoundTag entry = new CompoundTag(); entry.put("points", writePoints(variant));
                        variants.add(entry);
                    });
                    tag.put("variants", variants); result.add(tag);
                });
        return result;
    }

    private static ListTag writePoints(List<MagicGesturePoint> values) {
        ListTag points = new ListTag();
        values.forEach(point -> {
            CompoundTag entry = new CompoundTag(); entry.putDouble("x", point.x());
            entry.putDouble("y", point.y()); points.add(entry);
        });
        return points;
    }

    private static Map<MagicGestureTemplateTarget, MagicGestureTemplateLibrary.Template> readTemplates(ListTag list) {
        Map<MagicGestureTemplateTarget, MagicGestureTemplateLibrary.Template> result =
                new java.util.EnumMap<>(MagicGestureTemplateTarget.class);
        MagicGestureTemplateLibrary library = new MagicGestureTemplateLibrary();
        for (int index = 0; index < list.size(); index++) {
            CompoundTag tag = list.getCompound(index);
            MagicGestureTemplateTarget.fromSerialized(tag.getString("target")).ifPresent(target -> {
                try {
                    List<MagicGesturePoint> points = migrate(library, readPoints(tag.getList(
                            "points", Tag.TAG_COMPOUND)));
                    List<List<MagicGesturePoint>> variants = new ArrayList<>();
                    ListTag variantTags = tag.getList("variants", Tag.TAG_COMPOUND);
                    for (int variantIndex = 0; variantIndex < variantTags.size(); variantIndex++) {
                        variants.add(migrate(library, readPoints(variantTags.getCompound(variantIndex)
                                .getList("points", Tag.TAG_COMPOUND))));
                    }
                    if (variants.isEmpty()) variants.add(points);
                    result.put(target, new MagicGestureTemplateLibrary.Template(target, points,
                            variants, tag.getDouble("threshold")));
                } catch (IllegalArgumentException ignored) {
                    // Old or malformed optional templates do not block loading the player's spells.
                }
            });
        }
        return Map.copyOf(result);
    }

    private static List<MagicGesturePoint> readPoints(ListTag tags) {
        List<MagicGesturePoint> points = new ArrayList<>(tags.size());
        for (int index = 0; index < tags.size(); index++) {
            CompoundTag point = tags.getCompound(index);
            points.add(new MagicGesturePoint(point.getDouble("x"), point.getDouble("y"), index));
        }
        return points;
    }

    private static List<MagicGesturePoint> migrate(MagicGestureTemplateLibrary library,
                                                    List<MagicGesturePoint> points) {
        return points.size() == MagicGestureTemplateLibrary.POINT_COUNT ? List.copyOf(points)
                : library.normalize(points);
    }
}
