package io.github.kxyr0.echoitemattachments.forge.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class ItemAttachmentServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<ItemAttachmentService> services = new ArrayList<>();

    public ItemAttachmentServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void register(ItemAttachmentService service) {
        services.add(Objects.requireNonNull(service, "service"));
    }

    public List<ItemAttachmentService> services() {
        return List.copyOf(services);
    }
}
