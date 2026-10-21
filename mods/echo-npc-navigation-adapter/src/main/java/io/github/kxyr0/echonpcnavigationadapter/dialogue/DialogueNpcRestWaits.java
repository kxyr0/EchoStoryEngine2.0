package io.github.kxyr0.echonpcnavigationadapter.dialogue;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.command.DialogueCommandPayload;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.rest.command.NpcSleepRequest;
import io.github.kxyr0.echonpc.api.rest.command.NpcWakeRequest;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpcnavigationadapter.script.NpcNavigationPayloads;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

final class DialogueNpcRestWaits {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<String, Pending> pending = new LinkedHashMap<>();

    void sleepAt(MinecraftServer server, DialogueService dialogue, DialogueSessionView session,
            DialogueExternalWait wait) {
        try {
            var command = NpcNavigationPayloads.sleep(DialogueCommandPayload.decode(wait.payload()));
            var lookup = EchoNpcServiceBridge.lookup(server);
            if (lookup.service().isEmpty()) {
                LOGGER.warn("ENPC-NAV-DIALOGUE-SLEEP-NO-SERVICE session={} message={}",
                        session.sessionId().value(), lookup.message());
                dialogue.cancel(session.sessionId());
                return;
            }
            NpcResults.Sleep result = lookup.service().orElseThrow().sleepAt(new NpcSleepRequest(command.actorId(),
                    command.worldId(), command.bedPosition(), command.expectedBlock(),
                    mutationId("sleep", session, wait)));
            LOGGER.info("ENPC-NAV-DIALOGUE-SLEEP session={} actor={} bed={} status={} message={}",
                    session.sessionId().value(), command.actorId().value(), command.bedPosition(),
                    result.status(), result.message());
            if (result.status() == NpcResults.SleepStatus.QUEUED && result.operationId().isPresent()) {
                pending.put(result.operationId().orElseThrow(),
                        new Pending(server, dialogue, session.sessionId(), session.revision(), wait.waitKey()));
            } else if (result.status() == NpcResults.SleepStatus.SLEEPING
                    || result.status() == NpcResults.SleepStatus.ALREADY_SLEEPING) {
                resume(dialogue, session.sessionId(), session.revision(), wait.waitKey());
            } else {
                dialogue.cancel(session.sessionId());
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("ENPC-NAV-DIALOGUE-SLEEP-FAILED session={}", session.sessionId().value(), exception);
            dialogue.cancel(session.sessionId());
        }
    }

    void wakeUp(MinecraftServer server, DialogueService dialogue, DialogueSessionView session,
            DialogueExternalWait wait) {
        try {
            var actor = NpcNavigationPayloads.actor(DialogueCommandPayload.decode(wait.payload()));
            var lookup = EchoNpcServiceBridge.lookup(server);
            if (lookup.service().isEmpty()) {
                LOGGER.warn("ENPC-NAV-DIALOGUE-WAKE-NO-SERVICE session={} message={}",
                        session.sessionId().value(), lookup.message());
                dialogue.cancel(session.sessionId());
                return;
            }
            NpcResults.Wake result = lookup.service().orElseThrow().wakeUp(new NpcWakeRequest(actor,
                    mutationId("wake", session, wait)));
            LOGGER.info("ENPC-NAV-DIALOGUE-WAKE session={} actor={} status={} message={}",
                    session.sessionId().value(), actor.value(), result.status(), result.message());
            if (result.status() == NpcResults.WakeStatus.QUEUED && result.operationId().isPresent()) {
                pending.put(result.operationId().orElseThrow(),
                        new Pending(server, dialogue, session.sessionId(), session.revision(), wait.waitKey()));
            } else if (result.status() == NpcResults.WakeStatus.WOKE_UP
                    || result.status() == NpcResults.WakeStatus.ALREADY_AWAKE) {
                resume(dialogue, session.sessionId(), session.revision(), wait.waitKey());
            } else {
                dialogue.cancel(session.sessionId());
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("ENPC-NAV-DIALOGUE-WAKE-FAILED session={}", session.sessionId().value(), exception);
            dialogue.cancel(session.sessionId());
        }
    }

    void tick(MinecraftServer server) {
        EchoNpcServiceBridge.lookup(server).service().ifPresent(service -> tick(server, service));
    }

    void close(MinecraftServer server) {
        pending.entrySet().removeIf(entry -> entry.getValue().server() == server);
    }

    private void tick(MinecraftServer server, NpcService service) {
        for (Map.Entry<String, Pending> entry : pending.entrySet().stream().toList()) {
            if (entry.getValue().server() == server) {
                service.operation(entry.getKey()).operation().ifPresent(operation -> complete(entry.getKey(),
                        entry.getValue(), operation.status()));
            }
        }
    }

    private void complete(String operationId, Pending wait, NpcResults.OperationStatus status) {
        if (status == NpcResults.OperationStatus.QUEUED || status == NpcResults.OperationStatus.RUNNING) {
            return;
        }
        LOGGER.info("ENPC-NAV-DIALOGUE-REST-COMPLETE session={} operation={} status={}",
                wait.sessionId().value(), operationId, status);
        if (status == NpcResults.OperationStatus.SUCCEEDED) {
            resume(wait.dialogue(), wait.sessionId(), wait.expectedRevision(), wait.waitKey());
        } else {
            wait.dialogue().cancel(wait.sessionId());
        }
        pending.remove(operationId);
    }

    private void resume(DialogueService dialogue, DialogueSessionId sessionId, int expectedRevision, String waitKey) {
        dialogue.resumeExternal(new DialogueExternalResumeRequest(sessionId, expectedRevision, waitKey));
    }

    private ActorMutationId mutationId(String action, DialogueSessionView session, DialogueExternalWait wait) {
        String raw = action + "|" + session.sessionId().value() + "|" + wait.waitKey();
        return new ActorMutationId("dialogue:" + action + "_" + UUID.nameUUIDFromBytes(
                raw.getBytes(StandardCharsets.UTF_8)));
    }

    private record Pending(
            MinecraftServer server,
            DialogueService dialogue,
            DialogueSessionId sessionId,
            int expectedRevision,
            String waitKey) {
    }
}
