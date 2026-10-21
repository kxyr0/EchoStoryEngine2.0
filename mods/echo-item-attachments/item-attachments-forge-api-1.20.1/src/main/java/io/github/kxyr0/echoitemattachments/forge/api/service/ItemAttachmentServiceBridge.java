package io.github.kxyr0.echoitemattachments.forge.api.service;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class ItemAttachmentServiceBridge {
    private ItemAttachmentServiceBridge() {
    }

    public static LookupResult lookup(MinecraftServer server) {
        ItemAttachmentServiceLookupEvent event =
                new ItemAttachmentServiceLookupEvent(Objects.requireNonNull(server, "server"));
        MinecraftForge.EVENT_BUS.post(event);
        if (event.services().size() != 1) return new LookupResult(Status.UNAVAILABLE, Optional.empty());
        return new LookupResult(Status.RESOLVED, Optional.of(event.services().get(0)));
    }

    public record LookupResult(Status status, Optional<ItemAttachmentService> service) {
    }

    public enum Status {
        RESOLVED,
        UNAVAILABLE
    }
}
