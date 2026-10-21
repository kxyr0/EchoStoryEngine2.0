package io.github.kxyr0.echoevents.forge.api;

import io.github.kxyr0.echoevents.api.delivery.EventDelivery;
import io.github.kxyr0.echoevents.api.delivery.EventReactionPort;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class EventReactionDispatchEvent extends Event {
    private final MinecraftServer server;
    private final EventDelivery delivery;
    private EventReactionPort.Acceptance acceptance = EventReactionPort.Acceptance.RETRY;
    private boolean resolved;
    public EventReactionDispatchEvent(MinecraftServer server, EventDelivery delivery) {
        this.server = Objects.requireNonNull(server, "server");
        this.delivery = Objects.requireNonNull(delivery, "delivery");
    }
    public MinecraftServer server() { return server; }
    public EventDelivery delivery() { return delivery; }
    public EventReactionPort.Acceptance acceptance() { return acceptance; }
    public void resolve(EventReactionPort.Acceptance result) {
        if (resolved) throw new IllegalStateException("Duplicate reaction consumer");
        acceptance = Objects.requireNonNull(result, "result");
        resolved = true;
    }
}
