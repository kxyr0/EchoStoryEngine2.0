package io.github.kxyr0.echonpcnavigationadapter.dialogue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.command.DialogueCommandPayload;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeRequest;
import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeStatus;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

final class DialogueSkinWaits {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<String, Pending> skinPending =
            new LinkedHashMap<>();

    void changeSkin(
            MinecraftServer server,
            DialogueService service,
            DialogueSessionView session,
            DialogueExternalWait wait) {
        try {
            Map<String, String> payload =
                    DialogueCommandPayload.decode(wait.payload());

            String actorId = payload.get("npc");
            String skinId = payload.get("skin");

            if (actorId == null || actorId.isBlank()
                    || skinId == null || skinId.isBlank()) {
                LOGGER.warn(
                        "ENPC-NAV-DIALOGUE-SKIN-INVALID session={} payload={}",
                        session.sessionId().value(),
                        wait.payload());

                service.cancel(session.sessionId());
                return;
            }

            var lookup = EchoNpcServiceBridge.lookup(server);

            if (lookup.service().isEmpty()) {
                LOGGER.warn(
                        "ENPC-NAV-DIALOGUE-SKIN-NO-SERVICE session={} message={}",
                        session.sessionId().value(),
                        lookup.message());

                service.cancel(session.sessionId());
                return;
            }

            var result = lookup.service()
                    .orElseThrow()
                    .changeSkin(new NpcSkinChangeRequest(
                            new ActorId(actorId),
                            new NpcSkinId(skinId),
                            new ActorMutationId(
                                    skinCommandId(session, wait))));

            LOGGER.info(
                    "ENPC-NAV-DIALOGUE-SKIN session={} actor={} skin={} status={} message={}",
                    session.sessionId().value(),
                    actorId,
                    skinId,
                    result.status(),
                    result.message());

            if (result.status() == NpcSkinChangeStatus.QUEUED
                    && result.operationId().isPresent()) {
                skinPending.put(
                        result.operationId().orElseThrow(),
                        new Pending(
                                server,
                                service,
                                session.sessionId(),
                                session.revision(),
                                wait.waitKey()));
            } else if (result.status()
                    == NpcSkinChangeStatus.ALREADY_ACTIVE
                    || result.status()
                    == NpcSkinChangeStatus.COMMITTED) {
                resume(
                        service,
                        session.sessionId(),
                        session.revision(),
                        wait.waitKey());
            } else {
                service.cancel(session.sessionId());
            }
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "ENPC-NAV-DIALOGUE-SKIN-FAILED session={}",
                    session.sessionId().value(),
                    exception);

            service.cancel(session.sessionId());
        }
    }

    void tickSkins(
            MinecraftServer server,
            NpcService service) {
        for (Map.Entry<String, Pending> entry
                : skinPending.entrySet().stream().toList()) {
            if (entry.getValue().server() != server) {
                continue;
            }

            service.operation(entry.getKey())
                    .operation()
                    .ifPresent(operation -> completeSkin(
                            entry.getKey(),
                            entry.getValue(),
                            operation.status()));
        }
    }

    void clear() {
        skinPending.clear();
    }

    private void completeSkin(
            String operationId,
            Pending wait,
            NpcResults.OperationStatus status) {
        if (status == NpcResults.OperationStatus.QUEUED
                || status == NpcResults.OperationStatus.RUNNING) {
            return;
        }

        LOGGER.info(
                "ENPC-NAV-DIALOGUE-SKIN-COMPLETE session={} operation={} status={}",
                wait.sessionId().value(),
                operationId,
                status);

        if (status == NpcResults.OperationStatus.SUCCEEDED) {
            resume(
                    wait.service(),
                    wait.sessionId(),
                    wait.expectedRevision(),
                    wait.waitKey());
        } else {
            wait.service().cancel(wait.sessionId());
        }

        skinPending.remove(operationId);
    }

    private void resume(
            DialogueService service,
            DialogueSessionId sessionId,
            int expectedRevision,
            String waitKey) {
        service.resumeExternal(
                new DialogueExternalResumeRequest(
                        sessionId,
                        expectedRevision,
                        waitKey));
    }

    private String skinCommandId(
        DialogueSessionView session,
        DialogueExternalWait wait) {
    String raw =
            session.sessionId().value()
                    + "|"
                    + wait.waitKey();

    return "echo_npc:skin_"
            + UUID.nameUUIDFromBytes(
                    raw.getBytes(StandardCharsets.UTF_8))
            .toString()
            .replace("-", "");
	}	

    private record Pending(
            MinecraftServer server,
            DialogueService service,
            DialogueSessionId sessionId,
            int expectedRevision,
            String waitKey) {
    }
}
