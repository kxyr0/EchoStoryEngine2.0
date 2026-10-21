package io.github.kxyr0.echoplayer.core.recovery;

import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import java.util.Objects;
import java.util.Optional;

/**
 * Chooses the safest profile state after restart or damaged source data.
 */
public final class PlayerProfileRecovery {
    public PlayerProfileSnapshot recover(Optional<PlayerProfileSnapshot> persisted,
                                         PlayerProfileSnapshot recreated) {
        Objects.requireNonNull(persisted, "persisted");
        Objects.requireNonNull(recreated, "recreated");
        return persisted.orElse(recreated);
    }
}
