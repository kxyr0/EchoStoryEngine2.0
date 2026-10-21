package io.github.kxyr0.echoplayersleep.forge.api.transition;

import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import io.github.kxyr0.echoplayersleep.api.world.Ticks;
import net.minecraft.server.level.ServerPlayer;

public interface PlayerSleepTransitionClientService {
    void closeEyes(ServerPlayer player, PlayerSleepRevision revision, Ticks duration);

    default void openEyes(ServerPlayer player, PlayerSleepRevision revision, Ticks duration,
            boolean sleepyBlink) {
        openEyes(player, revision, duration, sleepyBlink, true);
    }

    void openEyes(ServerPlayer player, PlayerSleepRevision revision, Ticks duration,
            boolean sleepyBlink, boolean showHud);
}
