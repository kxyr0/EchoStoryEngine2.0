package io.github.kxyr0.echotime.script.dialogue;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.forge.api.wait.DialogueExternalWaitDispatchEvent;
import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceAccess;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeWaitCompletedEvent;
import io.github.kxyr0.echotime.script.action.TimeActionExecutor;
import io.github.kxyr0.echotime.script.action.TimeActionResult;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class TimeDialogueController implements AutoCloseable {
    private final MinecraftServer server;
    private final TimeDialoguePauses pauses;
    private final Map<TimeWaitId, Pending> waits = new LinkedHashMap<>();

    public TimeDialogueController(MinecraftServer server) {
        this.server = server;
        pauses = new TimeDialoguePauses(server);
    }

    public void dispatch(DialogueExternalWaitDispatchEvent event) {
        if (!event.externalWait().waitType().startsWith("echo_time:")) return;
        var time = EchoTimeServiceAccess.find(server);
        var dialogue = TimeDialogueServices.find(server);
        if (time.isEmpty() || dialogue.isEmpty()) return;
        TimeDialogueContext context = new TimeDialogueContext(event.session(), event.externalWait());
        TimeActionResult result = new TimeActionExecutor(time.orElseThrow()).execute(
                event.externalWait().waitType(), context.payload(), context.actionContext());
        trackPauseLifecycle(dialogue.orElseThrow(), event, result);
        if (result.status() == TimeActionResult.Status.WAITING) {
            waits.put(result.waitId().orElseThrow(), new Pending(
                    event.session().sessionId(), event.externalWait().waitKey()));
        } else resume(dialogue.orElseThrow(), event.session().sessionId(), event.externalWait().waitKey());
    }

    public void waitCompleted(EchoTimeWaitCompletedEvent event) {
        if (event.server() != server) return;
        Pending pending = waits.remove(event.waitId());
        if (pending == null) return;
        EchoTimeServiceAccess.find(server).ifPresent(time -> time.waits().complete(event.waitId()));
        TimeDialogueServices.find(server).ifPresent(dialogue ->
                resume(dialogue, pending.sessionId(), pending.waitKey()));
    }

    private void trackPauseLifecycle(DialogueService service, DialogueExternalWaitDispatchEvent event,
            TimeActionResult result) {
        if (result.status() != TimeActionResult.Status.SUCCESS) return;
        if (event.externalWait().waitType().equals("echo_time:pause")) {
            pauses.observe(service, event.session().sessionId());
        } else if (event.externalWait().waitType().equals("echo_time:resume")) {
            pauses.releaseObservation(event.session().sessionId());
        }
    }

    private void resume(DialogueService service, DialogueSessionId sessionId, String waitKey) {
        service.query(sessionId).session().ifPresent(session -> service.resumeExternal(
                new DialogueExternalResumeRequest(sessionId, session.revision(), waitKey)));
    }

    @Override public void close() { waits.clear(); pauses.close(); }

    private record Pending(DialogueSessionId sessionId, String waitKey) { }
}
