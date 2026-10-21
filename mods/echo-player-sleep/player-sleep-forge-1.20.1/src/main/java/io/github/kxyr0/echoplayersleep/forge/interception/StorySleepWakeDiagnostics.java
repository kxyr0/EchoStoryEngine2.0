package io.github.kxyr0.echoplayersleep.forge.interception;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import java.util.Arrays;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

public final class StorySleepWakeDiagnostics {
    private static final Logger LOGGER = LogUtils.getLogger();

    private StorySleepWakeDiagnostics() {
    }

    public static void logBlockedServerPlayerStop(
            ServerPlayer player,
            PlayerSleepSessionSnapshot session,
            boolean wakeImmediately,
            boolean updateLevel) {
        LOGGER.warn("Echo Player Sleep blocked ServerPlayer.stopSleepInBed player={} uuid={} status={} "
                        + "ownsBedPose={} permit={} wakeImmediately={} updateLevel={} stack={}",
                player.getGameProfile().getName(), player.getUUID(), session.status(),
                session.ownsBedPose(), StoryWakePermit.isAllowed(player.getUUID()),
                wakeImmediately, updateLevel, stack());
    }

    public static void logBlockedLivingStop(ServerPlayer player, PlayerSleepSessionSnapshot session) {
        LOGGER.warn("Echo Player Sleep blocked LivingEntity.stopSleeping player={} uuid={} status={} "
                        + "ownsBedPose={} permit={} stack={}",
                player.getGameProfile().getName(), player.getUUID(), session.status(),
                session.ownsBedPose(), StoryWakePermit.isAllowed(player.getUUID()), stack());
    }

    private static String stack() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .skip(3L)
                .limit(8L)
                .map(element -> element.getClassName() + "#" + element.getMethodName()
                        + ":" + element.getLineNumber())
                .collect(Collectors.joining(" <- "));
    }
}
