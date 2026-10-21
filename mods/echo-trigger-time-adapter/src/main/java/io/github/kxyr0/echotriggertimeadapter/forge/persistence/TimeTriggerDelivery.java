package io.github.kxyr0.echotriggertimeadapter.forge.persistence;

import io.github.kxyr0.echotrigger.api.subscription.TriggerSubscriptionTargetKind;
import java.util.Objects;
import java.util.Optional;

public record TimeTriggerDelivery(
        String deliveryId,
        String occurrenceId,
        String subscriptionId,
        String triggerId,
        TriggerSubscriptionTargetKind targetKind,
        String targetId,
        Optional<String> audienceId,
        long day,
        String time,
        long intervalStart,
        long intervalEnd,
        String crossingReason,
        TimeDeliveryStatus status) {
    public TimeTriggerDelivery {
        deliveryId = require(deliveryId, "deliveryId");
        occurrenceId = require(occurrenceId, "occurrenceId");
        subscriptionId = require(subscriptionId, "subscriptionId");
        triggerId = require(triggerId, "triggerId");
        targetKind = Objects.requireNonNull(targetKind, "targetKind");
        targetId = require(targetId, "targetId");
        audienceId = Objects.requireNonNull(audienceId, "audienceId").map(value -> require(value, "audienceId"));
        if (day < 1L) throw new IllegalArgumentException("day must be positive");
        time = require(time, "time");
        if (intervalStart < 0L || intervalEnd < intervalStart) {
            throw new IllegalArgumentException("invalid crossing interval");
        }
        crossingReason = require(crossingReason, "crossingReason");
        status = Objects.requireNonNull(status, "status");
    }

    public TimeTriggerDelivery delivered() {
        return new TimeTriggerDelivery(deliveryId, occurrenceId, subscriptionId, triggerId, targetKind,
                targetId, audienceId, day, time, intervalStart, intervalEnd, crossingReason,
                TimeDeliveryStatus.DELIVERED);
    }

    private static String require(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(name + " cannot be blank");
        return checked;
    }
}
