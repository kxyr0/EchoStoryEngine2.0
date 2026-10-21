package io.github.kxyr0.echoplayersleep.core.scenario;

import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepBedSnapshot;
import java.util.Objects;

public record PlayerSleepBedClaim(boolean claimed, PlayerSleepBedSnapshot snapshot) {
    public PlayerSleepBedClaim {
        if (claimed) {
            snapshot = Objects.requireNonNull(snapshot, "snapshot");
        }
    }

    public static PlayerSleepBedClaim skipped() {
        return new PlayerSleepBedClaim(false, null);
    }

    public static PlayerSleepBedClaim claimed(PlayerSleepBedSnapshot snapshot) {
        return new PlayerSleepBedClaim(true, snapshot);
    }
}
