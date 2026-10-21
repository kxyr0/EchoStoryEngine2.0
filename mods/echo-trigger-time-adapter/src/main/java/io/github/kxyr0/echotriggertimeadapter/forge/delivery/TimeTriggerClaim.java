package io.github.kxyr0.echotriggertimeadapter.forge.delivery;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.api.id.TriggerCommandId;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.status.TriggerResultCode;
import io.github.kxyr0.echotrigger.forge.api.runtime.TriggerServiceBridge;
import io.github.kxyr0.echotriggertimeadapter.forge.persistence.TimeTriggerDelivery;
import java.util.Map;

final class TimeTriggerClaim {
    boolean claim(TimeTriggerDelivery delivery, TriggerServiceBridge trigger, long serverTick) {
        TriggerOwnerId owner = new TriggerOwnerId("time:" + delivery.deliveryId());
        var result = trigger.runtime().start(TriggerId.parse(delivery.triggerId()), owner,
                new TriggerCommandId("time-occurrence:" + delivery.deliveryId()));
        boolean accepted = result.code() == TriggerResultCode.STARTED || result.code() == TriggerResultCode.EXISTING;
        if (!accepted) return false;
        trigger.events().applyEvent(TriggerEvent.vanilla(owner, serverTick, "Time.At", Map.of(
                "provider", "echo_time:occurrences",
                "event", "AT",
                "occurrenceId", delivery.occurrenceId(),
                "subscriptionId", delivery.subscriptionId(),
                "day", Long.toString(delivery.day()),
                "time", delivery.time(),
                "intervalStart", Long.toString(delivery.intervalStart()),
                "intervalEnd", Long.toString(delivery.intervalEnd()),
                "crossingReason", delivery.crossingReason())));
        return true;
    }
}
