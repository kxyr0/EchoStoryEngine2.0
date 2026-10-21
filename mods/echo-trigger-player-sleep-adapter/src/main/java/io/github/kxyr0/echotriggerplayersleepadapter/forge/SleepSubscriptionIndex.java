package io.github.kxyr0.echotriggerplayersleepadapter.forge;

import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventKind;
import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventSnapshot;
import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import java.util.List;

public final class SleepSubscriptionIndex {
    private static final String PROVIDER = "echo_player_sleep:events";
    private final List<TriggerStorySubscription> subscriptions;

    private SleepSubscriptionIndex(List<TriggerStorySubscription> subscriptions) {
        this.subscriptions = List.copyOf(subscriptions);
    }

    public static SleepSubscriptionIndex empty() {
        return new SleepSubscriptionIndex(List.of());
    }

    public static SleepSubscriptionIndex from(List<TriggerStorySubscription> subscriptions) {
        return new SleepSubscriptionIndex(subscriptions);
    }

    public List<TriggerStorySubscription> matches(PlayerSleepEventSnapshot event) {
        List<TriggerStorySubscription> matching = subscriptions.stream()
                .filter(subscription -> subscription.selector().sameEvent(PROVIDER,
                        event.kind().name(), event.scenarioId().value()))
                .filter(subscription -> subscription.selector().acceptsSleepCount(event.sleepCount()))
                .toList();
        int strongestType = matching.stream()
                .mapToInt(subscription -> specificityType(subscription.selector()))
                .max()
                .orElse(-1);
        long strongestThreshold = matching.stream()
                .filter(subscription -> specificityType(subscription.selector()) == strongestType)
                .mapToLong(subscription -> specificityThreshold(subscription.selector()))
                .max()
                .orElse(0L);
        return matching.stream()
                .filter(subscription -> specificityType(subscription.selector()) == strongestType)
                .filter(subscription -> specificityThreshold(subscription.selector()) == strongestThreshold)
                .toList();
    }

    public List<TriggerStorySubscription> armCandidates(long sleepCount) {
        List<TriggerStorySubscription> matching = subscriptions.stream()
                .filter(subscription -> subscription.selector().sameEvent(PROVIDER,
                        PlayerSleepEventKind.CONFIRMED.name(), subscription.selector().scenarioId()))
                .filter(subscription -> subscription.selector().acceptsSleepCount(sleepCount))
                .toList();
        int strongestType = matching.stream()
                .mapToInt(subscription -> specificityType(subscription.selector()))
                .max()
                .orElse(-1);
        long strongestThreshold = matching.stream()
                .filter(subscription -> specificityType(subscription.selector()) == strongestType)
                .mapToLong(subscription -> specificityThreshold(subscription.selector()))
                .max()
                .orElse(0L);
        return matching.stream()
                .filter(subscription -> specificityType(subscription.selector()) == strongestType)
                .filter(subscription -> specificityThreshold(subscription.selector()) == strongestThreshold)
                .toList();
    }

    private static int specificityType(TriggerEventSelector selector) {
        if (selector.sleepCount().isPresent()) {
            return 2;
        }
        return selector.sleepCountAtLeast().isPresent() ? 1 : 0;
    }

    private static long specificityThreshold(TriggerEventSelector selector) {
        if (selector.sleepCount().isPresent()) {
            return selector.sleepCount().orElseThrow();
        }
        return selector.sleepCountAtLeast().isPresent() ? selector.sleepCountAtLeast().orElseThrow() : 0L;
    }
}
