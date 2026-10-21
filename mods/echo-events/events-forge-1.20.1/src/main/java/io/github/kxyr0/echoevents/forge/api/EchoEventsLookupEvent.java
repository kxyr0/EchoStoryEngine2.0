package io.github.kxyr0.echoevents.forge.api;

import io.github.kxyr0.echoevents.core.runtime.EventDispatcher;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/** Forge is only the service-discovery boundary; occurrences live in the Echo journal. */
public final class EchoEventsLookupEvent extends Event {
    private final MinecraftServer server;
    private Optional<EventDispatcher> service = Optional.empty();
    public EchoEventsLookupEvent(MinecraftServer server) { this.server = Objects.requireNonNull(server, "server"); }
    public MinecraftServer server() { return server; }
    public Optional<EventDispatcher> service() { return service; }
    public void register(EventDispatcher dispatcher) {
        if (service.isPresent()) throw new IllegalStateException("Duplicate Echo event service");
        service = Optional.of(dispatcher);
    }
}
