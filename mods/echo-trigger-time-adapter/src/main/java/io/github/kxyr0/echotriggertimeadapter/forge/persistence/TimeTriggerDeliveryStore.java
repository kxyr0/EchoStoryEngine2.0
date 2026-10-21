package io.github.kxyr0.echotriggertimeadapter.forge.persistence;

import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

public final class TimeTriggerDeliveryStore {
    private final MinecraftServer server;
    private final TimeTriggerDeliveryData data;

    public TimeTriggerDeliveryStore(MinecraftServer server) {
        this.server = server;
        data = TimeTriggerDeliveryData.get(server.overworld());
    }

    public Optional<TimeTriggerDelivery> find(String deliveryId) { return data.find(deliveryId); }
    public List<TimeTriggerDelivery> pending() { return data.pending(); }
    public long occurrenceGeneration() { return data.occurrenceGeneration(); }

    public long advanceOccurrenceGeneration() {
        long generation = data.advanceOccurrenceGeneration();
        flush();
        return generation;
    }

    public void pending(TimeTriggerDelivery delivery) {
        data.put(delivery);
        flush();
    }

    public void pendingAll(List<TimeTriggerDelivery> deliveries) {
        if (deliveries.isEmpty()) return;
        deliveries.forEach(data::put);
        flush();
    }

    public void delivered(TimeTriggerDelivery delivery) {
        data.put(delivery.delivered());
        flush();
    }

    private void flush() { server.overworld().getDataStorage().save(); }
}
