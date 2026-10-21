package io.github.kxyr0.echodialogue.forge.domain;

import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echoevents.api.event.EventMutationPolicy;
import io.github.kxyr0.echoevents.forge.api.EchoEvents;
import io.github.kxyr0.echoevents.persistence.codec.EventOutboxCodec;
import java.util.HashMap;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class DialogueEventOutbox {
    private static final String KEY = "domainEvent.prepared";
    private DialogueEventOutbox() { }

    public static DialogueCheckpoint pin(DialogueCheckpoint checkpoint) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { return checkpoint; }
        var runtime = EchoEvents.service(server);
        var publications = DialogueDomainEvents.occurrences(checkpoint, true).stream()
                .map(runtime::prepare).toList();
        var metadata = new HashMap<>(checkpoint.metadata());
        metadata.put(KEY, new EventOutboxCodec().encode(publications));
        return new DialogueCheckpoint(checkpoint.sessionId(), checkpoint.dialogueId(), checkpoint.compiledFingerprint(),
                checkpoint.audienceId(), checkpoint.phase(), checkpoint.revision(), checkpoint.currentNodeId(),
                checkpoint.presentationKind(), checkpoint.lineOrdinal(), checkpoint.createdAtEpochMillis(),
                checkpoint.updatedAtEpochMillis(), checkpoint.terminalResult(), checkpoint.cancellationReason(),
                metadata, checkpoint.repositoryRevision());
    }

    public static boolean publish(DialogueCheckpoint checkpoint) {
        if (!checkpoint.metadata().containsKey(KEY)) { return false; }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { return true; }
        var runtime = EchoEvents.service(server);
        new EventOutboxCodec().decode(checkpoint.metadata().get(KEY))
                .forEach(value -> runtime.publish(value, EventMutationPolicy.NORMAL));
        return true;
    }
}
