package io.github.kxyr0.echobackgroundnpc.navigation;

import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundLocationId;
import io.github.kxyr0.echobackgroundnpc.forge.api.location.BackgroundLocationResolution;
import io.github.kxyr0.echobackgroundnpc.forge.api.location.BackgroundLocationResolverEvent;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

/** Queries all canonical providers and rejects ambiguous registration-order fallback. */
public final class ExactLocationRegistry {
    public BackgroundLocationResolution resolve(MinecraftServer server, BackgroundLocationId id) {
        BackgroundLocationResolverEvent event = new BackgroundLocationResolverEvent();
        MinecraftForge.EVENT_BUS.post(event);
        List<BackgroundLocationResolution> found = event.resolvers().stream()
                .map(value -> value.resolve(server, id))
                .filter(value -> value.status() == BackgroundLocationResolution.Status.FOUND).toList();
        if (found.isEmpty()) return BackgroundLocationResolution.missing(id);
        if (found.size() > 1) return new BackgroundLocationResolution(id,
                BackgroundLocationResolution.Status.AMBIGUOUS, java.util.Optional.empty(),
                java.util.Optional.empty());
        return found.get(0);
    }
}
