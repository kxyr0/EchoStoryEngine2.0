package io.github.kxyr0.echonpc.forge.api;

import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Public explicit Forge bridge for resolving the server-scoped neutral NpcService.
 */
public final class EchoNpcServiceBridge {
    private EchoNpcServiceBridge() {
    }

    public static EchoNpcServiceLookupResult lookup(MinecraftServer server) {
        EchoNpcServiceLookupEvent event = new EchoNpcServiceLookupEvent(Objects.requireNonNull(server, "server"));
        MinecraftForge.EVENT_BUS.post(event);
        if (event.providers().isEmpty()) {
            return new EchoNpcServiceLookupResult(EchoNpcServiceLookupResult.Status.UNAVAILABLE,
                    java.util.Optional.empty(), "Echo NPC service is unavailable.");
        }
        if (event.providers().size() > 1) {
            return new EchoNpcServiceLookupResult(EchoNpcServiceLookupResult.Status.DUPLICATE_PROVIDER,
                    java.util.Optional.empty(), "More than one Echo NPC service provider responded.");
        }
        return new EchoNpcServiceLookupResult(EchoNpcServiceLookupResult.Status.RESOLVED,
                java.util.Optional.of(event.providers().get(0)), "Echo NPC service resolved.");
    }
}
