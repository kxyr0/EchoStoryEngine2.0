package io.github.kxyr0.echoteleportmagic.forge.integration;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.context.EventAudience;
import io.github.kxyr0.echoevents.api.context.EventSource;
import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.event.EventMutationPolicy;
import io.github.kxyr0.echoevents.api.identity.EventIds;
import io.github.kxyr0.echoevents.api.payload.EventPayload.TeleportFact;
import io.github.kxyr0.echoevents.forge.api.EchoEvents;
import io.github.kxyr0.echoteleportmagic.api.TeleportExecution;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;

public final class TeleportEvents implements Consumer<TeleportExecution> {
    private final MinecraftServer server;
    public TeleportEvents(MinecraftServer server) { this.server = server; }
    @Override public void accept(TeleportExecution e) {
        EventKind kind = switch (e.state()) {
            case DEPARTING -> EventKind.TELEPORT_STARTED;
            case COMMITTED -> EventKind.TELEPORT_COMMITTED;
            case COMPLETED -> EventKind.TELEPORT_COMPLETED;
            case CANCELLED -> EventKind.TELEPORT_CANCELLED;
            case FAILED -> EventKind.TELEPORT_FAILED;
            default -> throw new IllegalArgumentException("Not a lifecycle transition");
        };
        var source = new EventSource(new EventIds.ResourceId("echo:golden_reference"),
                new EventIds.ExecutionId(e.id().value().toString()), new EventIds.ExecutionId(e.rootOwner()),
                new EventIds.TransitionId(kind.stableId()), new EventAudience.World(), "echo_teleport_magic",
                server.overworld().getGameTime(), OptionalLong.empty(), "1", Optional.empty());
        EchoEvents.publish(server, EventEnvelope.occurrence(source,
                new TeleportFact(kind, new EventIds.SymbolId(e.subjectReference()))), EventMutationPolicy.NORMAL);
    }
}
