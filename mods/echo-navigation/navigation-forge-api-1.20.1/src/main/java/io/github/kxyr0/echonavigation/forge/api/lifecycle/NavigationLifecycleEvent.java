package io.github.kxyr0.echonavigation.forge.api.lifecycle;

import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

public final class NavigationLifecycleEvent extends Event {
    private final Kind kind;
    private final NavigationSnapshot snapshot;

    public NavigationLifecycleEvent(Kind kind, NavigationSnapshot snapshot) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    public Kind kind() {
        return kind;
    }

    public NavigationSnapshot snapshot() {
        return snapshot;
    }

    public enum Kind {
        MOVEMENT_STARTED,
        MOVEMENT_PROGRESSED,
        MOVEMENT_ARRIVED,
        MOVEMENT_STOPPED,
        MOVEMENT_FAILED
    }
}
