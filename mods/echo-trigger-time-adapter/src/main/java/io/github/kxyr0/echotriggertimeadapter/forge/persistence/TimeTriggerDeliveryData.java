package io.github.kxyr0.echotriggertimeadapter.forge.persistence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

public final class TimeTriggerDeliveryData extends SavedData {
    private static final String NAME = "echo_trigger_time_deliveries";
    private final Map<String, TimeTriggerDelivery> deliveries = new LinkedHashMap<>();
    private final TimeTriggerDeliveryCodec codec = new TimeTriggerDeliveryCodec();
    private long occurrenceGeneration;

    public static TimeTriggerDeliveryData get(net.minecraft.server.level.ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TimeTriggerDeliveryData::load,
                TimeTriggerDeliveryData::new, NAME);
    }

    public static TimeTriggerDeliveryData load(CompoundTag root) {
        TimeTriggerDeliveryData data = new TimeTriggerDeliveryData();
        data.occurrenceGeneration = root.getLong("occurrenceGeneration");
        ListTag list = root.getList("deliveries", Tag.TAG_COMPOUND);
        for (Tag value : list) {
            TimeTriggerDelivery delivery = data.codec.decode((CompoundTag) value);
            data.deliveries.put(delivery.deliveryId(), delivery);
        }
        return data;
    }

    public Optional<TimeTriggerDelivery> find(String deliveryId) {
        return Optional.ofNullable(deliveries.get(deliveryId));
    }

    public void put(TimeTriggerDelivery delivery) { deliveries.put(delivery.deliveryId(), delivery); setDirty(); }

    public List<TimeTriggerDelivery> pending() {
        return deliveries.values().stream().filter(value -> value.status() == TimeDeliveryStatus.PENDING).toList();
    }

    public long occurrenceGeneration() { return occurrenceGeneration; }

    public long advanceOccurrenceGeneration() {
        occurrenceGeneration = Math.addExact(occurrenceGeneration, 1L);
        setDirty();
        return occurrenceGeneration;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        root.putLong("occurrenceGeneration", occurrenceGeneration);
        ListTag list = new ListTag();
        deliveries.values().stream().sorted(java.util.Comparator.comparing(TimeTriggerDelivery::deliveryId))
                .map(codec::encode).forEach(list::add);
        root.put("deliveries", list);
        return root;
    }
}
