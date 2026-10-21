package io.github.kxyr0.echotriggervisibilityadapter.forge;

import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VisibilitySubscriptionIndex {
    private static final String PROVIDER = "echo_visibility:events";
    private final Map<String, List<TriggerStorySubscription>> byExactKey;

    private VisibilitySubscriptionIndex(Map<String, List<TriggerStorySubscription>> byExactKey) {
        this.byExactKey = Map.copyOf(byExactKey);
    }

    public static VisibilitySubscriptionIndex empty() {
        return new VisibilitySubscriptionIndex(Map.of());
    }

    public static VisibilitySubscriptionIndex from(List<TriggerStorySubscription> subscriptions) {
        Map<String, List<TriggerStorySubscription>> indexed = new LinkedHashMap<>();
        subscriptions.stream().filter(value -> value.selector().providerId().equals(PROVIDER))
                .forEach(value -> indexed.compute(value.selector().exactKey(), (key, existing) -> {
                    if (existing == null) {
                        return List.of(value);
                    }
                    var appended = new java.util.ArrayList<>(existing);
                    appended.add(value);
                    return List.copyOf(appended);
                }));
        return new VisibilitySubscriptionIndex(indexed);
    }

    public List<TriggerStorySubscription> matches(String eventKind, String watchId) {
        return byExactKey.getOrDefault(PROVIDER + "|" + eventKind + "|" + watchId, List.of());
    }

    public int size() {
        return byExactKey.size();
    }
}
