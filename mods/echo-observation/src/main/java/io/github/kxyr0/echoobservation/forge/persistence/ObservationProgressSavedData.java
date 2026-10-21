package io.github.kxyr0.echoobservation.forge.persistence;

import io.github.kxyr0.echoobservation.api.action.ObservationActionExecutionId;
import io.github.kxyr0.echoobservation.api.identity.ObservationCompletionId;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import io.github.kxyr0.echoobservation.core.progress.ObservationPlayerProgress;
import io.github.kxyr0.echoobservation.core.progress.ObservationProgressBook;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class ObservationProgressSavedData extends SavedData {
    private static final String NAME = "echo_observation_progress";
    private ObservationProgressBook book;
    private final Map<String, String> outboxes = new java.util.LinkedHashMap<>();
    private boolean overlayLoaded;

    ObservationProgressSavedData(ObservationProgressBook book) {
        this.book = book;
    }

    public static ObservationProgressSavedData get(MinecraftServer server) {
        var storage = server.overworld().getDataStorage();
        var data = storage.computeIfAbsent(ObservationProgressSavedData::load,
                () -> new ObservationProgressSavedData(new ObservationProgressBook()), NAME);
        if (!data.overlayLoaded) {
            data = ObservationDurableStore.load(server).orElse(data);
            data.overlayLoaded = true;
            storage.set(NAME, data);
        }
        return data;
    }
    public Map<String, String> outboxes() { return Map.copyOf(outboxes); }
    public void outbox(String completion, String encoded) { outboxes.put(completion, encoded); }
    public void acknowledgeOutbox(String completion) { outboxes.remove(completion); }
    public void restore(CompoundTag tag) {
        var restored = fromTag(tag);
        book = restored.book;
        outboxes.clear();
        outboxes.putAll(restored.outboxes);
        setDirty();
    }

    public ObservationProgressBook book() { return book; }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag players = new ListTag();
        book.entries().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                players.add(writePlayer(entry.getKey(), entry.getValue())));
        root.put("players", players);
        ListTag pending = new ListTag();
        outboxes.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            var value = new CompoundTag();
            value.putString("completion", entry.getKey());
            value.putByteArray("event", entry.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            pending.add(value);
        });
        root.put("eventOutboxes", pending);
        return root;
    }

    private static ObservationProgressSavedData load(CompoundTag root) {
        return fromTag(root);
    }

    public static ObservationProgressSavedData fromTag(CompoundTag root) {
        ObservationProgressBook book = new ObservationProgressBook();
        ListTag players = root.getList("players", Tag.TAG_COMPOUND);
        players.forEach(tag -> readPlayer((CompoundTag) tag, book));
        var data = new ObservationProgressSavedData(book);
        root.getList("eventOutboxes", Tag.TAG_COMPOUND).forEach(raw -> {
            var value = (CompoundTag) raw;
            data.outboxes.put(value.getString("completion"),
                    new String(value.getByteArray("event"), java.nio.charset.StandardCharsets.UTF_8));
        });
        return data;
    }

    private static CompoundTag writePlayer(UUID id, ObservationPlayerProgress progress) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("player", id);
        tag.put("active", strings(progress.activeResources().stream().map(Object::toString).toList()));
        tag.put("disabled", strings(progress.disabledObservations().stream().map(Object::toString).toList()));
        ListTag completions = new ListTag();
        progress.completions().forEach((observation, completion) -> {
            CompoundTag value = new CompoundTag();
            value.putString("observation", observation.toString());
            value.putString("completion", completion.value());
            completions.add(value);
        });
        tag.put("completed", completions);
        tag.put("receipts", strings(progress.receipts().stream().map(ObservationActionExecutionId::value).toList()));
        return tag;
    }

    private static void readPlayer(CompoundTag tag, ObservationProgressBook book) {
        if (!tag.hasUUID("player")) return;
        ObservationPlayerProgress progress = book.player(tag.getUUID("player"));
        tag.getList("active", Tag.TAG_STRING).forEach(value ->
                progress.restoreActive(ObservationResourceId.parse(value.getAsString())));
        tag.getList("disabled", Tag.TAG_STRING).forEach(value ->
                progress.restoreDisabled(ObservationId.parse(value.getAsString())));
        tag.getList("completed", Tag.TAG_COMPOUND).forEach(value -> {
            CompoundTag completion = (CompoundTag) value;
            progress.restoreCompletion(ObservationId.parse(completion.getString("observation")),
                    new ObservationCompletionId(completion.getString("completion")));
        });
        tag.getList("receipts", Tag.TAG_STRING).forEach(value ->
                progress.restoreReceipt(new ObservationActionExecutionId(value.getAsString())));
    }

    private static ListTag strings(java.util.List<String> values) {
        ListTag result = new ListTag();
        values.stream().sorted().map(StringTag::valueOf).forEach(result::add);
        return result;
    }
}
