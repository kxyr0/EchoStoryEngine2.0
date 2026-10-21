package io.github.kxyr0.echoplayersleep.core.session;

import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepBedSnapshot;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepOriginSnapshot;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import java.util.Objects;

public record VanillaSleepConfirmation(
        PlayerRef player,
        PlayerSleepBedSnapshot bed,
        PlayerSleepOriginSnapshot origin,
        long serverTick) {
    public VanillaSleepConfirmation {
        player = Objects.requireNonNull(player, "player");
        bed = Objects.requireNonNull(bed, "bed");
        origin = Objects.requireNonNull(origin, "origin");
    }
}
