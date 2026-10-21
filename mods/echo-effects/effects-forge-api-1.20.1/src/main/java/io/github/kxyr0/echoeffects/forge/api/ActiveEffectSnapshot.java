package io.github.kxyr0.echoeffects.forge.api;

import io.github.kxyr0.echoeffects.api.view.ActiveEffectView;
import java.util.Objects;

public record ActiveEffectSnapshot(ActiveEffectView view, long syncRevision) {
    public ActiveEffectSnapshot {
        view = Objects.requireNonNull(view, "view");
        if (syncRevision < 0L) {
            throw new IllegalArgumentException("Sync revision must not be negative.");
        }
    }
}
