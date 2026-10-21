package io.github.kxyr0.echotrigger.forge.api.domain;

import io.github.kxyr0.echoevents.api.delivery.EventDelivery;
import io.github.kxyr0.echoevents.api.delivery.EventReactionPort.Acceptance;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition.Action;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/** An adapter must durably deduplicate actionToken before returning ACCEPTED. */
public final class TriggerEventActionDispatchEvent extends Event {
    private final MinecraftServer server;
    private final EventDelivery delivery;
    private final Action action;
    private final String actionToken;
    private Acceptance acceptance = Acceptance.RETRY;
    private boolean resolved;
    private ResourceId canonicalSource;
    public TriggerEventActionDispatchEvent(MinecraftServer server, EventDelivery delivery,
            Action action, String actionToken) {
        this.server = Objects.requireNonNull(server, "server");
        this.delivery = Objects.requireNonNull(delivery, "delivery");
        this.action = Objects.requireNonNull(action, "action");
        this.actionToken = Objects.requireNonNull(actionToken, "actionToken");
    }
    public MinecraftServer server() { return server; }
    public EventDelivery delivery() { return delivery; }
    public Action action() { return action; }
    public String actionToken() { return actionToken; }
    public Acceptance acceptance() { return acceptance; }
    public ResourceId sourceResource() {
        return canonicalSource == null ? delivery.event().source().resource() : canonicalSource;
    }
    public void resolveSourceResource(ResourceId resource) {
        if (canonicalSource != null) {
            throw new IllegalStateException("Duplicate canonical source provider");
        }
        canonicalSource = Objects.requireNonNull(resource, "resource");
    }
    public void resolve(Acceptance value) {
        if (resolved) {
            throw new IllegalStateException("Duplicate event action consumer");
        }
        acceptance = Objects.requireNonNull(value, "value");
        resolved = true;
    }
}
