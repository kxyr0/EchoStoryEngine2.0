package io.github.kxyr0.echotime.script.dialogue;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.observation.DialogueObservationHandle;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.identity.TimePauseOwner;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceAccess;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

final class TimeDialoguePauses implements AutoCloseable {
    private final MinecraftServer server;
    private final Map<DialogueSessionId, DialogueObservationHandle> handles = new LinkedHashMap<>();

    TimeDialoguePauses(MinecraftServer server) { this.server = server; }

    void observe(DialogueService service, DialogueSessionId sessionId) {
        handles.computeIfAbsent(sessionId, id -> service.observeTerminal(id, event -> cleanup(id)));
    }

    void releaseObservation(DialogueSessionId sessionId) {
        DialogueObservationHandle handle = handles.remove(sessionId);
        if (handle != null) handle.close();
    }

    private void cleanup(DialogueSessionId sessionId) {
        EchoTimeServiceAccess.find(server).ifPresent(time -> time.mutations().resume(
                new TimePauseOwner("dialogue:" + sessionId.value()),
                new TimeMutationId("dialogue-terminal:" + sessionId.value())));
        releaseObservation(sessionId);
    }

    @Override public void close() { handles.values().forEach(DialogueObservationHandle::close); handles.clear(); }
}
