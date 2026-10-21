package io.github.kxyr0.echotriggertimeadapter.forge.schedule;

import io.github.kxyr0.echotime.api.schedule.TimeScheduleEntry;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import io.github.kxyr0.echotrigger.forge.api.runtime.TriggerServiceBridge;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TimeSubscriptionIndex {
    private final long catalogRevision;
    private final long occurrenceGeneration;
    private final Map<String, TriggerStorySubscription> subscriptions;
    private final List<TimeScheduleEntry> schedule;

    private TimeSubscriptionIndex(long catalogRevision, long occurrenceGeneration,
            Map<String, TriggerStorySubscription> subscriptions, List<TimeScheduleEntry> schedule) {
        this.catalogRevision = catalogRevision;
        this.occurrenceGeneration = occurrenceGeneration;
        this.subscriptions = Map.copyOf(subscriptions);
        this.schedule = List.copyOf(schedule);
    }

    public static TimeSubscriptionIndex empty() {
        return new TimeSubscriptionIndex(-1L, -1L, Map.of(), List.of());
    }

    public static TimeSubscriptionIndex from(TriggerServiceBridge trigger, long occurrenceGeneration) {
        TimeSelectorParser parser = new TimeSelectorParser();
        Map<String, TriggerStorySubscription> subscriptions = new LinkedHashMap<>();
        List<TriggerStorySubscription> matching = trigger.subscriptions().storySubscriptions().stream()
                .filter(value -> value.selector().providerId().equals("echo_time:occurrences"))
                .toList();
        List<TimeScheduleEntry> entries = matching.stream().flatMap(subscription ->
                parser.parse(subscription.selector()).stream().map(selector ->
                        entry(trigger, subscription, selector, occurrenceGeneration)))
                .toList();
        matching.forEach(value -> subscriptions.put(value.subscriptionId().value(), value));
        return new TimeSubscriptionIndex(trigger.subscriptions().catalogRevision(), occurrenceGeneration,
                subscriptions, entries);
    }

    public long catalogRevision() { return catalogRevision; }
    public long occurrenceGeneration() { return occurrenceGeneration; }
    public List<TimeScheduleEntry> schedule() { return schedule; }
    public Optional<TriggerStorySubscription> find(String selectorId) {
        return Optional.ofNullable(subscriptions.get(selectorId));
    }

    private static TimeScheduleEntry entry(TriggerServiceBridge trigger,
            TriggerStorySubscription subscription, TimeSelectorValue selector, long occurrenceGeneration) {
        long revision = trigger.definitions().revision(subscription.triggerId()).orElseThrow().value();
        String generation = revision + ":" + subscription.selector().fingerprint()
                + ":reset=" + occurrenceGeneration;
        return new TimeScheduleEntry(subscription.subscriptionId().value(), selector.time(), selector.day(),
                generation, 0);
    }
}
