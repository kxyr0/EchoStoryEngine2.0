package io.github.kxyr0.echotrigger.forge.api.runtime;

import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import java.util.List;

public interface TriggerSubscriptionCatalogView {
    default List<io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition> eventSubscriptions() {
        return List.of();
    }
    long catalogRevision();

    List<TriggerStorySubscription> storySubscriptions();

    List<TriggerStorySubscription> storySubscriptions(TriggerEventSelector selector);
}
