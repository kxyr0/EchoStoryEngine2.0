package io.github.kxyr0.echocutscene.forge.domain;

import io.github.kxyr0.echocutscene.api.runtime.CutsceneOutcome;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneStatus;
import io.github.kxyr0.echocutscene.core.lifecycle.CutscenePlayback;
import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.context.EventAudience;
import io.github.kxyr0.echoevents.api.context.EventSource;
import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.event.EventMutationPolicy;
import io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.identity.EventIds.TransitionId;
import io.github.kxyr0.echoevents.api.payload.CutsceneFact;
import io.github.kxyr0.echoevents.forge.api.EchoEvents;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class CutsceneDomainEvents {
    private CutsceneDomainEvents() { }

    public static void publish(ServerPlayer player, CutscenePlayback playback, CutsceneOutcome outcome) {
        CutsceneFact.Phase phase = phase(playback.status(), outcome);
        if (phase == null) {
            return;
        }
        var execution = new ExecutionId(playback.playbackId().value());
        var context = EchoEvents.service(player.getServer()).executionContext(execution);
        var source = new EventSource(new ResourceId(playback.definition().cutsceneId().toString()),
                execution, context.map(value -> value.root()).orElse(execution), new TransitionId("state-" + phase),
                new EventAudience.Player(player.getUUID()),
                "echo_cutscene", player.getServer().getTickCount(), OptionalLong.empty(),
                Long.toString(playback.definition().pinnedRevision().value()), Optional.empty());
        EchoEvents.publish(player.getServer(), EventEnvelope.occurrence(source, new CutsceneFact(phase)),
                EventMutationPolicy.NORMAL);
    }

    public static void inheritCommand(MinecraftServer server, String playbackId, String commandId) {
        var service = EchoEvents.service(server);
        service.executionContext(new ExecutionId(commandId)).ifPresent(context ->
                service.rememberContexts(java.util.Map.of(new ExecutionId(playbackId), context)));
    }

    public static boolean terminalCommitted(MinecraftServer server, String playback) {
        var events = EchoEvents.service(server).journalImage().occurrences();
        var execution = new ExecutionId(playback);
        return events.containsKey(EventEnvelope.identity("echo_cutscene", execution,
                new TransitionId("state-COMPLETED"), EventKind.CUTSCENE_COMPLETED))
                || events.containsKey(EventEnvelope.identity("echo_cutscene", execution,
                        new TransitionId("state-CANCELLED"), EventKind.CUTSCENE_CANCELLED));
    }

    private static CutsceneFact.Phase phase(CutsceneStatus status, CutsceneOutcome outcome) {
        if (outcome == CutsceneOutcome.COMPLETED) {
            return CutsceneFact.Phase.COMPLETED;
        }
        if (outcome == CutsceneOutcome.STOPPED) {
            return CutsceneFact.Phase.CANCELLED;
        }
        return status == CutsceneStatus.WAITING_FOR_CLIENT ? CutsceneFact.Phase.STARTED : null;
    }
}
