package io.github.kxyr0.echotriggertimeadapter.forge.persistence;

import io.github.kxyr0.echotrigger.api.subscription.TriggerSubscriptionTargetKind;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;

final class TimeTriggerDeliveryCodec {
    CompoundTag encode(TimeTriggerDelivery value) {
        CompoundTag tag = new CompoundTag();
        tag.putString("deliveryId", value.deliveryId());
        tag.putString("occurrenceId", value.occurrenceId());
        tag.putString("subscriptionId", value.subscriptionId());
        tag.putString("triggerId", value.triggerId());
        tag.putString("targetKind", value.targetKind().name());
        tag.putString("targetId", value.targetId());
        value.audienceId().ifPresent(audience -> tag.putString("audienceId", audience));
        tag.putLong("day", value.day());
        tag.putString("time", value.time());
        tag.putLong("intervalStart", value.intervalStart());
        tag.putLong("intervalEnd", value.intervalEnd());
        tag.putString("crossingReason", value.crossingReason());
        tag.putString("status", value.status().name());
        return tag;
    }

    TimeTriggerDelivery decode(CompoundTag tag) {
        return new TimeTriggerDelivery(tag.getString("deliveryId"), tag.getString("occurrenceId"),
                tag.getString("subscriptionId"), tag.getString("triggerId"),
                TriggerSubscriptionTargetKind.valueOf(tag.getString("targetKind")), tag.getString("targetId"),
                tag.contains("audienceId") ? Optional.of(tag.getString("audienceId")) : Optional.empty(),
                tag.getLong("day"), tag.getString("time"), tag.getLong("intervalStart"),
                tag.getLong("intervalEnd"), tag.getString("crossingReason"),
                TimeDeliveryStatus.valueOf(tag.getString("status")));
    }
}
