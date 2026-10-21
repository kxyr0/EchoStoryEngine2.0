package io.github.kxyr0.echoanimation.forge.domain;

import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackOutcome;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.TerminalRecord;
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

import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Optional;
import java.util.OptionalLong;

public final class AnimationDomainEvents {
    private AnimationDomainEvents() {}

    public static void completed(PlaybackRecord playback, TerminalRecord terminal) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || terminal.outcome != AnimationPlaybackOutcome.COMPLETED) {
            return;
        }
        var runtime = EchoEvents.service(server);
        var context = runtime.executionContext(new ExecutionId(playback.playbackId.value()))
                .or(() -> runtime.executionContext(new ExecutionId(playback.ownerId.value()))
                        .map(value -> value.asCaller(new ExecutionId(playback.ownerId.value()))));
        var source = new EventSource( new ResourceId(playback.animationId.value()),
                        new ExecutionId(playback.playbackId.value()), context.map(value -> value.root())
                                .orElseGet(() -> new ExecutionId(playback.ownerId.value())),
                        new TransitionId("completed"),
                        context.map(value -> value.audience()).orElseGet(EventAudience.World::new), "echo_animation",
                        terminal.completionTick, OptionalLong.empty(), playback.definitionFingerprint,
                        Optional.empty(), context.map(value -> value.caller()));
        var payload = new AnimationFact( new SymbolId(playback.subjectId.value()),
                        new SymbolId(playback.playbackId.value()), Optional.empty());
        EchoEvents.publish( server, EventEnvelope.occurrence(source, payload), EventMutationPolicy.NORMAL);
    }
}
