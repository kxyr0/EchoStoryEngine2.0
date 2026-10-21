package io.github.kxyr0.echoplayeractor.forge.api.service;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class EchoPlayerActorServiceBridge {
    private EchoPlayerActorServiceBridge() {
    }

    public static LookupResult lookup(MinecraftServer server) {
        PlayerActorServiceLookupEvent event =
                new PlayerActorServiceLookupEvent(Objects.requireNonNull(server, "server"));
        MinecraftForge.EVENT_BUS.post(event);
        if (event.services().size() != 1 || event.queryServices().size() != 1
                || event.leaseServices().size() != 1) {
            return new LookupResult(Status.UNAVAILABLE, Optional.empty(), Optional.empty(), Optional.empty());
        }
        return new LookupResult(Status.RESOLVED, Optional.of(event.services().get(0)),
                Optional.of(event.queryServices().get(0)), Optional.of(event.leaseServices().get(0)));
    }

    public record LookupResult(
            Status status,
            Optional<PlayerActorService> service,
            Optional<PlayerActorQueryService> queryService,
            Optional<PlayerControlLeaseService> leaseService) {
    }

    public enum Status {
        RESOLVED,
        UNAVAILABLE
    }
}
