package io.github.kxyr0.echodialogue.forge.api;

import io.github.kxyr0.echodialogue.api.service.DialogueService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Synchronous server-scoped lookup event for the public Echo Dialogue service.
 */
public final class EchoDialogueServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<DialogueService> providers = new ArrayList<>();

    public EchoDialogueServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(DialogueService service) {
        providers.add(Objects.requireNonNull(service, "service"));
    }

    public List<DialogueService> providers() {
        return List.copyOf(providers);
    }
}
