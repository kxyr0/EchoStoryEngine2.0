package io.github.kxyr0.echodebug.forge.client;

import io.github.kxyr0.echodebug.core.selection.TriggerSelectionSnapshot;
import java.util.Optional;

public final class ClientSelectionMirror {
    private Optional<TriggerSelectionSnapshot> snapshot = Optional.empty();

    public void apply(TriggerSelectionSnapshot snapshot) {
        this.snapshot = Optional.of(snapshot);
    }

    public Optional<TriggerSelectionSnapshot> snapshot() {
        return snapshot;
    }

    public long revision() {
        return snapshot.map(TriggerSelectionSnapshot::revision).orElse(0L);
    }
}
