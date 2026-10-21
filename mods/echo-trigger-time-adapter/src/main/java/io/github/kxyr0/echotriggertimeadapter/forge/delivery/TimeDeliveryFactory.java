package io.github.kxyr0.echotriggertimeadapter.forge.delivery;

import io.github.kxyr0.echotime.api.schedule.TimeOccurrenceCrossed;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import io.github.kxyr0.echotriggertimeadapter.forge.persistence.TimeDeliveryStatus;
import io.github.kxyr0.echotriggertimeadapter.forge.persistence.TimeTriggerDelivery;
import java.util.Optional;

final class TimeDeliveryFactory {
    TimeTriggerDelivery create(TimeOccurrenceCrossed occurrence,
            TriggerStorySubscription subscription, Optional<String> audience) {
        String audienceKey = audience.orElse("global");
        String id = TimeDeliveryIds.delivery(occurrence.occurrenceId().value(),
                subscription.subscriptionId().value(), audienceKey, subscription.fireOnlyOnce());
        return new TimeTriggerDelivery(id, occurrence.occurrenceId().value(),
                subscription.subscriptionId().value(), subscription.triggerId().value(),
                subscription.targetKind(), subscription.targetId(), audience,
                occurrence.occurrence().day().value(), occurrence.occurrence().timeOfDay().formatHHmm(),
                occurrence.intervalStart().absoluteMicroseconds(), occurrence.intervalEnd().absoluteMicroseconds(),
                occurrence.reason().name(), TimeDeliveryStatus.PENDING);
    }
}
