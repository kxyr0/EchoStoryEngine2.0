package io.github.kxyr0.echodialogue.forge.domain;

import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryLoadResult;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.core.session.DialogueTransitionStatus;
import io.github.kxyr0.echoevents.api.context.EventAudience;
import io.github.kxyr0.echoevents.api.context.EventSource;
import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.event.EventMutationPolicy;
import io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId;
import io.github.kxyr0.echoevents.api.identity.EventIds.TransitionId;
import io.github.kxyr0.echoevents.api.payload.DialogueFact;
import io.github.kxyr0.echoevents.forge.api.EchoEvents;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Outbox metadata is committed atomically with the authoritative Dialogue checkpoint. */
public final class DialogueDomainEvents {
    private DialogueDomainEvents() { }

    public static DialogueCheckpoint stamp(DialogueCheckpoint checkpoint, DialogueTransition transition) {
        return stamp(checkpoint, transition, checkpoint.revision() == 0);
    }
    public static DialogueCheckpoint stamp(DialogueCheckpoint checkpoint, DialogueTransition transition,
            boolean initial) {
        var metadata = new HashMap<>(checkpoint.metadata());
        var server = ServerLifecycleHooks.getCurrentServer();
        metadata.put("domainEvent.tick", Long.toString(server == null ? 0 : server.getTickCount()));
        if (initial) {
            metadata.put("domainEvent.started", "true");
        }
        if (transition.status() == DialogueTransitionStatus.STARTED) {
            metadata.put("domainEvent.phase", "STARTED");
        }
        if (transition.status() == DialogueTransitionStatus.COMPLETED) {
            metadata.put("domainEvent.phase", "COMPLETED");
        }
        if (transition.status() == DialogueTransitionStatus.CANCELLED) {
            metadata.put("domainEvent.phase", "CANCELLED");
        }
        if (transition.status() == DialogueTransitionStatus.FAILED) {
            metadata.put("domainEvent.phase", "FAILED");
            metadata.put("domainEvent.failure", transition.diagnostics().stream().findFirst()
                    .map(value -> value.code()).orElse("EDLG_EXECUTION_FAILED"));
        }
        transition.selectedChoice().ifPresent(choice -> metadata.put("domainEvent.choice", choice.value()));
        return new DialogueCheckpoint(checkpoint.sessionId(), checkpoint.dialogueId(), checkpoint.compiledFingerprint(),
                checkpoint.audienceId(), checkpoint.phase(), checkpoint.revision(), checkpoint.currentNodeId(),
                checkpoint.presentationKind(), checkpoint.lineOrdinal(), checkpoint.createdAtEpochMillis(),
                checkpoint.updatedAtEpochMillis(), checkpoint.terminalResult(), checkpoint.cancellationReason(),
                metadata, checkpoint.repositoryRevision());
    }
    public static List<EventEnvelope<DialogueFact>> occurrences(DialogueCheckpoint checkpoint, boolean recovery) {
        List<EventEnvelope<DialogueFact>> result = new ArrayList<>();
        if (!recovery && checkpoint.metadata().containsKey("domainEvent.started")) {
            result.add(event(checkpoint, DialogueFact.lifecycle(DialogueFact.Phase.STARTED)));
        }
        if (checkpoint.metadata().containsKey("domainEvent.phase")) {
            var phase = DialogueFact.Phase.valueOf(checkpoint.metadata().get("domainEvent.phase"));
            if (phase == DialogueFact.Phase.FAILED) {
                var failure = new SymbolId(checkpoint.metadata().get("domainEvent.failure"));
                result.add(event(checkpoint, DialogueFact.failed(failure)));
            } else if (phase == DialogueFact.Phase.STARTED) {
                if (!recovery && !checkpoint.metadata().containsKey("domainEvent.started")) {
                    result.add(event(checkpoint, DialogueFact.lifecycle(phase)));
                }
            } else {
                result.add(event(checkpoint, DialogueFact.lifecycle(phase)));
            }
        }
        if (checkpoint.metadata().containsKey("domainEvent.choice")) {
            result.add(event(checkpoint,
                DialogueFact.choice(new SymbolId(checkpoint.metadata().get("domainEvent.choice")))));
        }
        return List.copyOf(result);
    }
    public static void publishRecovered(List<DialogueRepositoryLoadResult> loaded) {
        loaded.forEach(result -> result.checkpoint().ifPresent(checkpoint -> publish(checkpoint, true)));
    }
    public static void publish(DialogueCheckpoint checkpoint, boolean recovery) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        boolean pinned = DialogueEventOutbox.publish(checkpoint);
        for (var occurrence : occurrences(checkpoint, recovery)) {
            if (pinned && occurrence.kind().durable()) { continue; }
            EchoEvents.publish(server, occurrence, EventMutationPolicy.NORMAL);
        }
    }
    private static EventEnvelope<DialogueFact> event(DialogueCheckpoint checkpoint, DialogueFact payload) {
        var execution = new ExecutionId(checkpoint.sessionId().value());
        var root = new ExecutionId(checkpoint.metadata().getOrDefault("domainEvent.root", execution.value()));
        var source = new EventSource(new ResourceId(checkpoint.dialogueId().toString()), execution, root,
                new TransitionId("revision-" + checkpoint.revision()),
                new EventAudience.Player(UUID.fromString(checkpoint.audienceId().value())), "echo_dialogue",
                Long.parseLong(checkpoint.metadata().getOrDefault("domainEvent.tick", "0")), OptionalLong.empty(),
                checkpoint.compiledFingerprint().value(), Optional.empty(),
                Optional.ofNullable(checkpoint.metadata().get("domainEvent.caller")).map(ExecutionId::new));
        return EventEnvelope.occurrence(source, payload);
    }
}
