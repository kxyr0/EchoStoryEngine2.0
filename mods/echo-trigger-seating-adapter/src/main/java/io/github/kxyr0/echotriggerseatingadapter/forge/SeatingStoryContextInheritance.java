package io.github.kxyr0.echotriggerseatingadapter.forge;

import io.github.kxyr0.echoseating.api.event.PlayerSeatEvent;
import io.github.kxyr0.echoseating.forge.api.context.SeatingStoryContextBridge;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SeatingStoryContextInheritance {
    public void install(
            ExecutionId storyExecutionId,
            PlayerSeatEvent event,
            TriggerStorySubscription subscription,
            String triggerExecutionId) {
        SeatingStoryContextBridge.install(storyExecutionId.asString(),
                context(storyExecutionId, event, subscription, triggerExecutionId));
    }

    private Map<String, String> context(
            ExecutionId storyExecutionId,
            PlayerSeatEvent event,
            TriggerStorySubscription subscription,
            String triggerExecutionId) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("player", event.player().uuid().toString());
        values.put("owner", "player:" + event.player().uuid());
        values.put("seat.zone", event.zoneId().value());
        event.slotId().ifPresent(slot -> values.put("seat.slot", slot.value()));
        values.put("seat.event", event.eventId().value());
        values.put("seat.anchor", event.anchor().blockPosition().toString());
        values.put("seat.vehicle", event.anchor().vehicleUuid().toString());
        values.put("seat.vehicleType", event.vehicleEntityTypeId());
        values.put("seat.revision", Long.toString(event.occupancyRevision().value()));
        values.put("trigger.subscription", subscription.subscriptionId().value());
        values.put("trigger.execution", triggerExecutionId);
        values.put("story.execution", storyExecutionId.asString());
        return Map.copyOf(values);
    }
}
