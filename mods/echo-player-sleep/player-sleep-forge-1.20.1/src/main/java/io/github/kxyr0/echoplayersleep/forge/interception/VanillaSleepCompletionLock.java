package io.github.kxyr0.echoplayersleep.forge.interception;

import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class VanillaSleepCompletionLock {
    private VanillaSleepCompletionLock() {
    }

    public static boolean preventsVanillaCompletion(ServerPlayer player) {
        return StorySleepRegistry.ownsBedPose(player)
                && !StoryWakePermit.isAllowed(player.getUUID());
    }

    public static boolean preventsVanillaWake(ServerPlayer player) {
        return blockedWakeSession(player).isPresent();
    }

    public static Optional<PlayerSleepSessionSnapshot> blockedWakeSession(ServerPlayer player) {
        if (StoryWakePermit.isAllowed(player.getUUID())) {
            return Optional.empty();
        }
        return StorySleepRegistry.active(player).filter(PlayerSleepSessionSnapshot::ownsBedPose);
    }
}
