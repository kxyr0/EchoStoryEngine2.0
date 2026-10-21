package io.github.kxyr0.echonavigation.forge.runtime;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import io.github.kxyr0.echonavigation.forge.api.lifecycle.NavigationLifecycleEvent;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;

final class NavigationLifecyclePublisher {
    private static final Logger LOGGER = LogUtils.getLogger();

    void started(NavigationSnapshot snapshot) {
        post(NavigationLifecycleEvent.Kind.MOVEMENT_STARTED, snapshot);
    }

    void progressed(NavigationSnapshot snapshot) {
        post(NavigationLifecycleEvent.Kind.MOVEMENT_PROGRESSED, snapshot);
    }

    void arrived(NavigationSnapshot snapshot) {
        post(NavigationLifecycleEvent.Kind.MOVEMENT_ARRIVED, snapshot);
    }

    void stopped(NavigationSnapshot snapshot) {
        post(NavigationLifecycleEvent.Kind.MOVEMENT_STOPPED, snapshot);
    }

    void failed(NavigationSnapshot snapshot) {
        post(NavigationLifecycleEvent.Kind.MOVEMENT_FAILED, snapshot);
    }

    private static void post(NavigationLifecycleEvent.Kind kind, NavigationSnapshot snapshot) {
        try {
            MinecraftForge.EVENT_BUS.post(new NavigationLifecycleEvent(kind, snapshot));
        } catch (RuntimeException exception) {
            LOGGER.warn("navigation_lifecycle_event_failed kind={} moveId={} subjectId={}",
                    kind, snapshot.moveId().value(), snapshot.subjectId().value(), exception);
        }
    }
}
