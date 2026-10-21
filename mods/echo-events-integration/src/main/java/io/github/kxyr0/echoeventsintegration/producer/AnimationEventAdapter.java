package io.github.kxyr0.echoeventsintegration.producer;

import io.github.kxyr0.echoanimation.forge.api.marker.AnimationMarkerEvent;
import io.github.kxyr0.echoevents.api.context.EventAudience;
import io.github.kxyr0.echoevents.api.context.EventSource;
import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.event.EventMutationPolicy;
import io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId;
import io.github.kxyr0.echoevents.api.identity.EventIds.TransitionId;
import io.github.kxyr0.echoevents.api.payload.AnimationFact;
import io.github.kxyr0.echoevents.forge.api.EchoEvents;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class AnimationEventAdapter {
    @SubscribeEvent
    public void marker(AnimationMarkerEvent event) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) return;
        var runtime = EchoEvents.service(server);
        var context = runtime.executionContext(new ExecutionId(event.playbackId().value()))
                .or(() -> runtime.executionContext(new ExecutionId(event.ownerId().value()))
                        .map(value -> value.asCaller(new ExecutionId(event.ownerId().value()))));
        var source = new EventSource(new ResourceId(event.animationId().toString()),
                new ExecutionId(event.playbackId().value()), context.map(value -> value.root())
                        .orElseGet(() -> new ExecutionId(event.ownerId().value())),
                new TransitionId(event.markerId().value() + ":" + event.phase() + ":" + event.phaseTick()
                        + ":" + event.playbackRevision() + ":tick:" + event.serverTick()), context.map(value -> value.audience())
                        .orElseGet(EventAudience.World::new), "echo_animation",
                event.serverTick(), OptionalLong.empty(), "unavailable;playback=" + event.playbackRevision(), Optional.empty(), context.map(value -> value.caller()));
        var payload = new AnimationFact(new SymbolId(event.subjectId().toString()), new SymbolId(event.playbackId().value()),
                Optional.of(new SymbolId(event.markerId().value())));
        EchoEvents.publish(server, EventEnvelope.occurrence(source, payload), EventMutationPolicy.NORMAL);
    }
}
