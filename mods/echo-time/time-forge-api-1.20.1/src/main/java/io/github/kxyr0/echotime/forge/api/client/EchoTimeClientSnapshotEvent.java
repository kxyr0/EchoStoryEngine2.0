package io.github.kxyr0.echotime.forge.api.client;

import io.github.kxyr0.echotime.api.snapshot.EchoTimeSnapshot;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

public final class EchoTimeClientSnapshotEvent extends Event {
    private final EchoTimeSnapshot snapshot;

    public EchoTimeClientSnapshotEvent(EchoTimeSnapshot snapshot) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    public EchoTimeSnapshot snapshot() {
        return snapshot;
    }
}
