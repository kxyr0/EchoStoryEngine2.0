package io.github.kxyr0.echotriggertimeadapter.forge.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echotrigger.api.subscription.TriggerSubscriptionTargetKind;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class TimeTriggerDeliveryDataTest {
    @Test
    void pendingAndDeliveredReceiptsRoundTripForRestartRecovery() {
        TimeTriggerDelivery delivery = new TimeTriggerDelivery("delivery", "occurrence", "subscription",
                "jdh:trigger", TriggerSubscriptionTargetKind.STORY, "jdh:story", Optional.empty(),
                2L, "09:50", 100L, 200L, "NATURAL_ADVANCE", TimeDeliveryStatus.PENDING);
        TimeTriggerDeliveryData data = new TimeTriggerDeliveryData();
        data.put(delivery);

        TimeTriggerDeliveryData pendingReload = TimeTriggerDeliveryData.load(data.save(new CompoundTag()));
        assertEquals(1, pendingReload.pending().size());
        pendingReload.put(delivery.delivered());
        TimeTriggerDeliveryData deliveredReload = TimeTriggerDeliveryData.load(
                pendingReload.save(new CompoundTag()));

        assertEquals(TimeDeliveryStatus.DELIVERED,
                deliveredReload.find("delivery").orElseThrow().status());
        assertEquals(0, deliveredReload.pending().size());
    }

    @Test
    void everyMemberOfCrossedBatchSurvivesBeforeDispatch() {
        TimeTriggerDelivery first = delivery("delivery-a", "occurrence-a", "09:50");
        TimeTriggerDelivery second = delivery("delivery-b", "occurrence-b", "10:00");
        TimeTriggerDeliveryData data = new TimeTriggerDeliveryData();
        data.put(first);
        data.put(second);
        TimeTriggerDeliveryData restored = TimeTriggerDeliveryData.load(data.save(new CompoundTag()));
        assertEquals(2, restored.pending().size());
        assertEquals(TimeDeliveryStatus.PENDING, restored.find("delivery-a").orElseThrow().status());
        assertEquals(TimeDeliveryStatus.PENDING, restored.find("delivery-b").orElseThrow().status());
    }

    @Test
    void occurrenceGenerationAdvancesAndSurvivesRestart() {
        TimeTriggerDeliveryData data = new TimeTriggerDeliveryData();

        assertEquals(0L, data.occurrenceGeneration());
        assertEquals(1L, data.advanceOccurrenceGeneration());

        TimeTriggerDeliveryData restored = TimeTriggerDeliveryData.load(data.save(new CompoundTag()));
        assertEquals(1L, restored.occurrenceGeneration());
        assertEquals(2L, restored.advanceOccurrenceGeneration());
    }

    private TimeTriggerDelivery delivery(String id, String occurrence, String time) {
        return new TimeTriggerDelivery(id, occurrence, "subscription", "jdh:trigger",
                TriggerSubscriptionTargetKind.STORY, "jdh:story", Optional.empty(), 2L, time,
                100L, 200L, "SCRIPT_TIME_JUMP", TimeDeliveryStatus.PENDING);
    }
}
