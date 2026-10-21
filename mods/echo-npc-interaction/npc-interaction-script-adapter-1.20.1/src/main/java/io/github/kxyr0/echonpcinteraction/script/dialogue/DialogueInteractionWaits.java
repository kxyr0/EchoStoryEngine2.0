package io.github.kxyr0.echonpcinteraction.script.dialogue;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.command.DialogueCommandPayload;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionResult;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionStatus;
import io.github.kxyr0.echonpcinteraction.script.NpcInteractionScriptFacade;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

public final class DialogueInteractionWaits {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final NpcInteractionScriptFacade facade = new NpcInteractionScriptFacade();
    private final Map<NpcInteractionCommandId, Pending> pending = new LinkedHashMap<>();

    public void interact(MinecraftServer server, DialogueService service, DialogueSessionView session,
            DialogueExternalWait wait) {
        String owner = session.sessionId().value() + ":" + wait.waitKey();
        Optional<NpcInteractionResult> result = facade.interact(server, owner,
                DialogueCommandPayload.decode(wait.payload()));
        if (result.isEmpty()) {
            LOGGER.warn("NPC_INTERACTION_DIALOGUE_SERVICE_MISSING session={} waitKey={}",
                    session.sessionId().value(), wait.waitKey());
            return;
        }
        var value = result.orElseThrow();
        LOGGER.info("NPC_INTERACTION_DIALOGUE_RESULT commandId={} status={} outcome={} failure={} message={}",
                value.commandId().value(), value.status(), value.outcome(),
                value.failure().map(Enum::name).orElse("none"), value.message());
        if (facade.isTerminal(value)) {
            complete(value.status(), new Pending(server, service, session.sessionId(),
                    session.revision(), wait.waitKey()));
        } else {
            pending.put(value.commandId(), new Pending(server, service, session.sessionId(),
                    session.revision(), wait.waitKey()));
        }
    }

    public void stop(MinecraftServer server, DialogueService service, DialogueSessionView session,
            DialogueExternalWait wait) {
        facade.stop(server, session.sessionId().value() + ":" + wait.waitKey(),
                DialogueCommandPayload.decode(wait.payload()));
        resume(service, session.sessionId(), session.revision(), wait.waitKey());
    }

    public void standUp(MinecraftServer server, DialogueService service, DialogueSessionView session,
            DialogueExternalWait wait) {
        facade.standUp(server, session.sessionId().value() + ":" + wait.waitKey(),
                DialogueCommandPayload.decode(wait.payload()));
        resume(service, session.sessionId(), session.revision(), wait.waitKey());
    }

    public void tick(MinecraftServer server) {
        for (Map.Entry<NpcInteractionCommandId, Pending> entry : pending.entrySet().stream().toList()) {
            if (entry.getValue().server() == server) {
                facade.result(server, entry.getKey()).filter(facade::isTerminal)
                        .ifPresent(result -> finish(entry.getKey(), result.status(), entry.getValue()));
            }
        }
    }

    public void close(MinecraftServer server) {
        pending.entrySet().removeIf(entry -> entry.getValue().server() == server);
    }

    private void finish(NpcInteractionCommandId commandId, NpcInteractionStatus status, Pending wait) {
        complete(status, wait);
        pending.remove(commandId);
    }

    private void complete(NpcInteractionStatus status, Pending wait) {
        if (status == NpcInteractionStatus.COMPLETED) {
            resume(wait.service(), wait.sessionId(), wait.expectedRevision(), wait.waitKey());
        } else {
            wait.service().cancel(wait.sessionId());
        }
    }

    private void resume(DialogueService service, DialogueSessionId sessionId, int expectedRevision, String waitKey) {
        service.resumeExternal(new DialogueExternalResumeRequest(sessionId, expectedRevision, waitKey));
    }

    private record Pending(
            MinecraftServer server,
            DialogueService service,
            DialogueSessionId sessionId,
            int expectedRevision,
            String waitKey) {
    }
}
