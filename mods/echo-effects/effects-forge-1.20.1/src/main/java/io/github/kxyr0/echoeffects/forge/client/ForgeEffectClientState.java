package io.github.kxyr0.echoeffects.forge.client;

import io.github.kxyr0.echoeffects.core.contribution.EffectContributionSnapshot;
import java.util.Objects;

public final class ForgeEffectClientState {
    private EffectContributionSnapshot snapshot = EffectContributionSnapshot.neutral();

    public void update(EffectContributionSnapshot snapshot) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    public EffectContributionSnapshot snapshot() {
        return snapshot;
    }
}
