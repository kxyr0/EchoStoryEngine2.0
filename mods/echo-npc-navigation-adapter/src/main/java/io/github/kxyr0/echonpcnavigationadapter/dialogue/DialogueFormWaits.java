package io.github.kxyr0.echonpcnavigationadapter.dialogue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.command.DialogueCommandPayload;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echonpc.api.form.NpcFormCommandId;
import io.github.kxyr0.echonpc.api.form.NpcFormStatus;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormChangeRequest;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpcnavigationadapter.script.NpcNavigationPayloads;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

final class DialogueFormWaits {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<String, Pending> formPending =
            new LinkedHashMap<>();

    void setForm(
            MinecraftServer server,
            DialogueService service,
            DialogueSessionView session,
            DialogueExternalWait wait) {
        try {
            var command = NpcNavigationPayloads.form(
                    DialogueCommandPayload.decode(wait.payload()));

            var lookup = EchoNpcServiceBridge.lookup(server);

            if (lookup.service().isEmpty()) {
                LOGGER.warn(
                        "ENPC-NAV-DIALOGUE-FORM-NO-SERVICE session={} message={}",
                        session.sessionId().value(),
                        lookup.message());

                service.cancel(session.sessionId());
                return;
            }

            var result = lookup.service()
                    .orElseThrow()
                    .setForm(new NpcFormChangeRequest(
                            command.actorId(),
                            command.formId(),
                            new NpcFormCommandId(
                                    formCommandId(session, wait))));

            LOGGER.info(
                    "ENPC-NAV-DIALOGUE-FORM session={} actor={} form={} status={} message={}",
                    session.sessionId().value(),
                    command.actorId().value(),
                    command.formId().value(),
                    result.status(),
                    result.message());

            if (result.status() == NpcFormStatus.QUEUED
                    && result.operationId().isPresent()) {
                formPending.put(
                        result.operationId().orElseThrow(),
                        new Pending(
                                server,
                                service,
                                session.sessionId(),
                                session.revision(),
                                wait.waitKey()));

                return;
            }

            if (result.status() == NpcFormStatus.ALREADY_ACTIVE
                    || result.status() == NpcFormStatus.COMMITTED) {
                resume(
                        service,
                        session.sessionId(),
                        session.revision(),
                        wait.waitKey());

                return;
            }

            service.cancel(session.sessionId());
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "ENPC-NAV-DIALOGUE-FORM-FAILED session={}",
                    session.sessionId().value(),
                    exception);

            service.cancel(session.sessionId());
        }
    }

    void tickForms(
            MinecraftServer server,
            NpcService service) {
        for (Map.Entry<String, Pending> entry
                : formPending.entrySet().stream().toList()) {
            if (entry.getValue().server() != server) {
                continue;
            }

            service.operation(entry.getKey())
                    .operation()
                    .ifPresent(operation -> completeForm(
                            entry.getKey(),
                            entry.getValue(),
                            operation.status()));
        }
    }

    void close(MinecraftServer server) {
        formPending.entrySet().removeIf(
                entry -> entry.getValue().server() == server);
    }

    private void completeForm(
            String operationId,
            Pending wait,
            NpcResults.OperationStatus status) {
        if (status == NpcResults.OperationStatus.QUEUED
                || status == NpcResults.OperationStatus.RUNNING) {
            return;
        }

        LOGGER.info(
                "ENPC-NAV-DIALOGUE-FORM-COMPLETE session={} operation={} status={}",
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

        formPending.remove(operationId);
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

    private String formCommandId(
            DialogueSessionView session,
            DialogueExternalWait wait) {
        String raw =
                session.sessionId().value()
                        + "|"
                        + wait.waitKey();

        return "dlg-form-"
                + UUID.nameUUIDFromBytes(
                        raw.getBytes(StandardCharsets.UTF_8));
    }

    private record Pending(
            MinecraftServer server,
            DialogueService service,
            DialogueSessionId sessionId,
            int expectedRevision,
            String waitKey) {
    }
}
