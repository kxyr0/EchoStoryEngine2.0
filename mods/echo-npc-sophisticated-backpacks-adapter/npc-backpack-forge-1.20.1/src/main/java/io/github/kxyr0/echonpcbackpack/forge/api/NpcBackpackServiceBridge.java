package io.github.kxyr0.echonpcbackpack.forge.api;

import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackService;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class NpcBackpackServiceBridge {
    private NpcBackpackServiceBridge() {
    }

    public static LookupResult lookup(MinecraftServer server) {
        NpcBackpackServiceLookupEvent event = new NpcBackpackServiceLookupEvent(
                Objects.requireNonNull(server, "server"));
        MinecraftForge.EVENT_BUS.post(event);
        if (event.services().isEmpty()) return new LookupResult(Status.UNAVAILABLE, Optional.empty());
        if (event.services().size() > 1) return new LookupResult(Status.DUPLICATE_PROVIDER, Optional.empty());
        return new LookupResult(Status.RESOLVED, Optional.of(event.services().get(0)));
    }

    public record LookupResult(Status status, Optional<NpcBackpackService> service) { }
    public enum Status { RESOLVED, UNAVAILABLE, DUPLICATE_PROVIDER }
}
