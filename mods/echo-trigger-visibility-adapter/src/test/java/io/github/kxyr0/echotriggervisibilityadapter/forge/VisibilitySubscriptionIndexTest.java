package io.github.kxyr0.echotriggervisibilityadapter.forge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import io.github.kxyr0.echotrigger.api.subscription.TriggerSubscriptionId;
import io.github.kxyr0.echotrigger.api.subscription.TriggerSubscriptionTargetKind;
import java.util.List;
import org.junit.jupiter.api.Test;

class VisibilitySubscriptionIndexTest {
    @Test
    void resolvesExactWatchWithoutCatalogScan() {
        TriggerStorySubscription father = subscription("father", "jdh:home/father");
        TriggerStorySubscription mother = subscription("mother", "jdh:home/mother");
        VisibilitySubscriptionIndex index = VisibilitySubscriptionIndex.from(List.of(father, mother));

        assertEquals(List.of(father), index.matches("LOST_SIGHT", "jdh:home/father"));
        assertEquals(2, index.size());
    }

    private static TriggerStorySubscription subscription(String name, String watch) {
        return new TriggerStorySubscription(new TriggerSubscriptionId("jdh:" + name),
                TriggerId.parse("jdh:" + name),
                TriggerEventSelector.of("echo_visibility:events", "LOST_SIGHT", watch),
                TriggerSubscriptionTargetKind.STORY, "jdh:story/" + name, true);
    }
}
