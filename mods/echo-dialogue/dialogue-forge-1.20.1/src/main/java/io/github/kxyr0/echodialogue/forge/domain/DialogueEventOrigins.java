package io.github.kxyr0.echodialogue.forge.domain;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueStartRequest;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryLoadResult;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DialogueEventOrigins {
    private final Map<DialogueSessionId, DialogueStartRequest.Origin> origins = new HashMap<>();

    public void remember(DialogueStartRequest request) {
        request.origin().ifPresent(value -> origins.put(request.sessionId(), value));
    }
    public DialogueCheckpoint stamp(DialogueCheckpoint checkpoint, DialogueTransition transition, boolean initial) {
        var stamped = DialogueDomainEvents.stamp(checkpoint, transition, initial);
        var origin = origins.get(checkpoint.sessionId());
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (origin != null && server != null) {
            var context = io.github.kxyr0.echoevents.forge.api.EchoEvents.service(server).executionContext(
                    new io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId(origin.rootExecution()));
            if (context.isPresent()) {
                origin = new DialogueStartRequest.Origin(context.get().root().value(), origin.callerExecution());
            }
        }
        if (origin == null) {
            return DialogueEventOutbox.pin(stamped);
        }
        var metadata = new HashMap<>(stamped.metadata());
        metadata.put("domainEvent.root", origin.rootExecution());
        metadata.put("domainEvent.caller", origin.callerExecution());
        return DialogueEventOutbox.pin(new DialogueCheckpoint(stamped.sessionId(), stamped.dialogueId(),
                stamped.compiledFingerprint(),
                stamped.audienceId(), stamped.phase(), stamped.revision(), stamped.currentNodeId(),
                stamped.presentationKind(), stamped.lineOrdinal(), stamped.createdAtEpochMillis(),
                stamped.updatedAtEpochMillis(), stamped.terminalResult(),
                stamped.cancellationReason(), metadata, stamped.repositoryRevision()));
    }
    public void recover(List<DialogueRepositoryLoadResult> loaded) {
        loaded.forEach(result -> result.checkpoint().ifPresent(checkpoint -> {
            var metadata = checkpoint.metadata();
            if (metadata.containsKey("domainEvent.root") && metadata.containsKey("domainEvent.caller")) {
                origins.put(checkpoint.sessionId(), new DialogueStartRequest.Origin(
                        metadata.get("domainEvent.root"), metadata.get("domainEvent.caller")));
            }
        }));
        DialogueDomainEvents.publishRecovered(loaded);
    }
}
