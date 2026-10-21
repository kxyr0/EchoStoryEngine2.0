package io.github.kxyr0.echoobservation.forge.domain;

import io.github.kxyr0.echoevents.api.context.EventAudience;
import io.github.kxyr0.echoevents.api.context.EventSource;
import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.event.EventMutationPolicy;
import io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.identity.EventIds.TransitionId;
import io.github.kxyr0.echoevents.api.payload.ObservationFact;
import io.github.kxyr0.echoevents.forge.api.EchoEvents;
import io.github.kxyr0.echoevents.persistence.codec.EventOutboxCodec;
import io.github.kxyr0.echoobservation.api.identity.ObservationCompletionId;
import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import io.github.kxyr0.echoobservation.forge.persistence.ObservationDurableStore;
import io.github.kxyr0.echoobservation.forge.persistence.ObservationProgressSavedData;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class ObservationDomainOutbox {
    private ObservationDomainOutbox() { }
    public static io.github.kxyr0.echoobservation.core.progress.ObservationCompletionResult mark(
            MinecraftServer server, ObservationProgressSavedData data, UUID player,
            ObservationDefinition definition,
            io.github.kxyr0.echoobservation.core.progress.ObservationCompletionFlow flow) {
        var result = new io.github.kxyr0.echoobservation.core.progress.ObservationCompletionResult[1];
        ObservationDurableStore.mutate(server, data, () -> {
            result[0] = flow.mark(player, definition, data.book().player(player));
            if (result[0].status()
                    == io.github.kxyr0.echoobservation.core.progress.ObservationCompletionResult.Status.COMPLETED) {
                prepare(server, data, player, definition, result[0].completionId().orElseThrow());
            }
        });
        return result[0];
    }
    public static void prepare(MinecraftServer server, ObservationProgressSavedData data, UUID player,
            ObservationDefinition definition, ObservationCompletionId completion) {
        var execution = new ExecutionId(completion.value());
        var source = new EventSource(new ResourceId(definition.id().toString()), execution, execution,
                new TransitionId("completed"), new EventAudience.Player(player), "echo_observation",
                server.getTickCount(), OptionalLong.empty(), Long.toString(definition.revision()), Optional.empty());
        var fact = EventEnvelope.occurrence(source, new ObservationFact(new io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId(completion.value())));
        var publication = EchoEvents.service(server).prepare(fact);
        data.outbox(completion.value(), new EventOutboxCodec().encode(List.of(publication)));
    }
    public static void publish(MinecraftServer server, ObservationProgressSavedData data) {
        boolean changed = false;
        for (var entry : data.outboxes().entrySet()) {
            var runtime = EchoEvents.service(server);
            new EventOutboxCodec().decode(entry.getValue())
                    .forEach(value -> runtime.publish(value, EventMutationPolicy.NORMAL));
            data.acknowledgeOutbox(entry.getKey());
            changed = true;
        }
        if (changed) {
            data.setDirty();
            ObservationDurableStore.commit(server, data);
        }
    }
}
