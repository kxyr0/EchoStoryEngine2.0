package io.github.kxyr0.echoconcentration.api.lifecycle;

import io.github.kxyr0.echoconcentration.api.ConcentrationSnapshot;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

public final class ConcentrationLifecycleEvent extends Event {
    public enum Kind { ENTERED, EXITED }

    private final Kind kind;
    private final ConcentrationSnapshot snapshot;

    public ConcentrationLifecycleEvent(Kind kind, ConcentrationSnapshot snapshot) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    public Kind kind() {
        return kind;
    }

    public ConcentrationSnapshot snapshot() {
        return snapshot;
    }
}
