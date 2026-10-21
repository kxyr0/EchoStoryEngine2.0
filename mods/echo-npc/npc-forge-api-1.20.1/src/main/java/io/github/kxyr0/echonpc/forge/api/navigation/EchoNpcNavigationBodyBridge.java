package io.github.kxyr0.echonpc.forge.api.navigation;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class EchoNpcNavigationBodyBridge {
    private EchoNpcNavigationBodyBridge() {
    }

    public static NpcNavigationBodyLookupResult lookup(MinecraftServer server, ActorId actorId) {
        NpcNavigationBodyLookupEvent event = new NpcNavigationBodyLookupEvent(
                Objects.requireNonNull(server, "server"), Objects.requireNonNull(actorId, "actorId"));
        MinecraftForge.EVENT_BUS.post(event);
        if (event.bodies().isEmpty()) {
            return new NpcNavigationBodyLookupResult(NpcNavigationBodyLookupResult.Status.NOT_FOUND,
                    Optional.empty(), "NPC navigation body is not available.");
        }
        if (event.bodies().size() > 1) {
            return new NpcNavigationBodyLookupResult(NpcNavigationBodyLookupResult.Status.DUPLICATE_PROVIDER,
                    Optional.empty(), "More than one NPC navigation body provider responded.");
        }
        return new NpcNavigationBodyLookupResult(NpcNavigationBodyLookupResult.Status.RESOLVED,
                Optional.of(event.bodies().get(0)), "NPC navigation body resolved.");
    }
}
