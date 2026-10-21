package io.github.kxyr0.echoplayersleep.forge.interception;

import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import io.github.kxyr0.echoplayersleep.forge.api.PlayerSleepServiceLookupEvent;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

public final class StorySleepRegistry {
    private StorySleepRegistry() {
    }

    public static Optional<PlayerSleepSessionSnapshot> active(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return Optional.empty();
        }
        PlayerSleepServiceLookupEvent lookup = new PlayerSleepServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(lookup);
        return lookup.bridge().flatMap(bridge -> bridge.query().active(new PlayerRef(player.getUUID())));
    }

    public static boolean ownsBedPose(ServerPlayer player) {
        return active(player).map(PlayerSleepSessionSnapshot::ownsBedPose).orElse(false);
    }
}
