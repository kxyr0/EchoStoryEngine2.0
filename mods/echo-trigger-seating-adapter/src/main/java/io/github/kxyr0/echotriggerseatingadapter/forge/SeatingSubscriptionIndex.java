package io.github.kxyr0.echotriggerseatingadapter.forge;

import io.github.kxyr0.echoseating.api.event.PlayerSeatEvent;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import java.util.List;
import java.util.Optional;

public final class SeatingSubscriptionIndex {
    private static final String PROVIDER = "echo_seating:events";
    private final List<TriggerStorySubscription> subscriptions;

    private SeatingSubscriptionIndex(List<TriggerStorySubscription> subscriptions) {
        this.subscriptions = List.copyOf(subscriptions);
    }

    public static SeatingSubscriptionIndex empty() {
        return new SeatingSubscriptionIndex(List.of());
    }

    public static SeatingSubscriptionIndex from(List<TriggerStorySubscription> subscriptions) {
        return new SeatingSubscriptionIndex(subscriptions);
    }

    public List<TriggerStorySubscription> matches(PlayerSeatEvent event) {
        return subscriptions.stream()
                .filter(subscription -> generic(subscription, event) || specific(subscription, event))
                .toList();
    }

    private boolean generic(TriggerStorySubscription subscription, PlayerSeatEvent event) {
        return subscription.selector().sameSeatingEvent(PROVIDER, event.kind().name(),
                event.zoneId().value(), Optional.empty());
    }

    private boolean specific(TriggerStorySubscription subscription, PlayerSeatEvent event) {
        return event.slotId().filter(slot -> subscription.selector().sameSeatingEvent(
                PROVIDER, event.kind().name(), event.zoneId().value(), Optional.of(slot.value()))).isPresent();
    }
}
