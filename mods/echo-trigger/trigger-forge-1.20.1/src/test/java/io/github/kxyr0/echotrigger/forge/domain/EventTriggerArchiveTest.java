package io.github.kxyr0.echotrigger.forge.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.identity.EventIds.EventSubscriptionId;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.subscription.EventFilter;
import io.github.kxyr0.echoevents.api.subscription.EventSubscription;
import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class EventTriggerArchiveTest {
    @TempDir Path root;
    @Test void pendingDeliveryKeepsOldActionsAcrossReloadAndDiskRestart() {
        var archive = new EventTriggerArchive(root);
        var old = definition("r1", "jdh:old_scene");
        var changed = definition("r2", "jdh:new_scene");
        archive.save(old);
        archive.save(changed);
        var restarted = new EventTriggerArchive(root);
        assertEquals(old, restarted.find(old.subscription()).orElseThrow());
        assertEquals(changed, restarted.find(changed.subscription()).orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> archive.save(definition("r1", "jdh:conflicting_scene")));
    }
    private EventTriggerDefinition definition(String revision, String scene) {
        var subscription = new EventSubscription(new EventSubscriptionId("jdh:father#When"),
                new EventFilter(EventKind.DIALOGUE_COMPLETED, new ResourceId("jdh:father"), Optional.empty()),
                revision, false);
        var source = new EventTriggerDefinition.Location("father.trigger.es", 4, 9);
        return new EventTriggerDefinition(subscription, source, List.of(new EventTriggerDefinition.Action(
                EventTriggerDefinition.Kind.SCENE_PLAY, new ResourceId(scene), source)));
    }
}
