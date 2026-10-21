package io.github.kxyr0.echotimehud.forge.client;

import io.github.kxyr0.echotime.api.snapshot.EchoTimeSnapshot;
import io.github.kxyr0.echotime.forge.api.client.EchoTimeClientSnapshotEvent;
import java.util.Optional;

public final class TimeHudSnapshotStore {
    private EchoTimeSnapshot current;

    public synchronized void accept(EchoTimeClientSnapshotEvent event) {
        EchoTimeSnapshot candidate = event.snapshot();
        if (current == null || candidate.instant().revision().compareTo(current.instant().revision()) >= 0) {
            current = candidate;
        }
    }

    public synchronized Optional<EchoTimeSnapshot> current() { return Optional.ofNullable(current); }
    public synchronized void clear() { current = null; }
}

