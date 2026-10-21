package io.github.kxyr0.echonpcinteraction.forge.api.service;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class EchoNpcInteractionServiceBridge {
    private EchoNpcInteractionServiceBridge() {
    }

    public static LookupResult lookup(MinecraftServer server) {
        EchoNpcInteractionServiceLookupEvent event =
                new EchoNpcInteractionServiceLookupEvent(Objects.requireNonNull(server, "server"));
        MinecraftForge.EVENT_BUS.post(event);
        if (event.services().size() != 1 || event.queryServices().size() != 1) {
            return new LookupResult(Status.UNAVAILABLE, Optional.empty(), Optional.empty());
        }
        return new LookupResult(Status.RESOLVED, Optional.of(event.services().get(0)),
                Optional.of(event.queryServices().get(0)));
    }

    public record LookupResult(
            Status status,
            Optional<NpcInteractionService> service,
            Optional<NpcInteractionQueryService> queryService) {
    }

    public enum Status {
        RESOLVED,
        UNAVAILABLE
    }
}
