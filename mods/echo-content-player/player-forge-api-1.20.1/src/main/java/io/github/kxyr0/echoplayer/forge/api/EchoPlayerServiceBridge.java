package io.github.kxyr0.echoplayer.forge.api;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Public explicit Forge bridge for resolving the server-scoped Echo player service.
 */
public final class EchoPlayerServiceBridge {
    private EchoPlayerServiceBridge() {
    }

    public static EchoPlayerServiceLookupResult lookup(MinecraftServer server) {
        EchoPlayerServiceLookupEvent event = new EchoPlayerServiceLookupEvent(
                Objects.requireNonNull(server, "server"));
        MinecraftForge.EVENT_BUS.post(event);
        if (event.providers().isEmpty()) {
            return new EchoPlayerServiceLookupResult(EchoPlayerServiceLookupResult.Status.UNAVAILABLE,
                    Optional.empty(), "Echo Content Player service is unavailable.");
        }
        if (event.providers().size() > 1) {
            return new EchoPlayerServiceLookupResult(EchoPlayerServiceLookupResult.Status.DUPLICATE_PROVIDER,
                    Optional.empty(), "More than one Echo Content Player service provider responded.");
        }
        return new EchoPlayerServiceLookupResult(EchoPlayerServiceLookupResult.Status.RESOLVED,
                Optional.of(event.providers().get(0)), "Echo Content Player service resolved.");
    }
}
