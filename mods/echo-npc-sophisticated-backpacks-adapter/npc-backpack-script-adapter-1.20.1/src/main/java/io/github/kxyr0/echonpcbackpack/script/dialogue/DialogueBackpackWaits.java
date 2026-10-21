package io.github.kxyr0.echonpcbackpack.script.dialogue;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.command.DialogueCommandPayload;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackCommandResult;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackOutcome;
import io.github.kxyr0.echonpcbackpack.script.NpcBackpackScriptFacade;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

public final class DialogueBackpackWaits {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<NpcBackpackCommandId, Pending> pending =
            new LinkedHashMap<>();

    public void dispatch(
            MinecraftServer server,
            DialogueService service,
            DialogueSessionView session,
            DialogueExternalWait wait,
            boolean equip) {

        String sessionId = session.sessionId().value();
        String owner = sessionId + ":" + wait.waitKey();
        String operation = equip ? "EQUIP" : "UNEQUIP";

        LOGGER.info(
                "ENPC-BACKPACK-DIALOGUE-DISPATCH "
                        + "operation={} session={} revision={} "
                        + "waitKey={} owner={} rawPayload={}",
                operation,
                sessionId,
                session.revision(),
                wait.waitKey(),
                owner,
                wait.payload());

        try {
            Map<String, String> payload =
                DialogueCommandPayload.decode(wait.payload());

            LOGGER.info(
                    "ENPC-BACKPACK-DIALOGUE-PAYLOAD "
                            + "operation={} session={} payload={}",
                    operation,
                    sessionId,
                    payload);

            NpcBackpackScriptFacade facade =
                    new NpcBackpackScriptFacade(server);

            NpcBackpackCommandResult result = equip
                    ? facade.equip(owner, payload)
                    : facade.unequip(owner, payload);

            LOGGER.info(
                    "ENPC-BACKPACK-DIALOGUE-RESULT "
                            + "operation={} session={} command={} "
                            + "terminal={} outcome={}",
                    operation,
                    sessionId,
                    result.commandId().value(),
                    result.terminal(),
                    result.outcome());

            Pending waiting = new Pending(
                    server,
                    service,
                    session.sessionId(),
                    session.revision(),
                    wait.waitKey(),
                    operation);

            if (result.terminal()) {
                LOGGER.info(
                        "ENPC-BACKPACK-DIALOGUE-IMMEDIATE "
                                + "operation={} session={} command={} "
                                + "outcome={}",
                        operation,
                        sessionId,
                        result.commandId().value(),
                        result.outcome());

                complete(result.outcome(), waiting);
                return;
            }

            Pending previous = pending.put(
                    result.commandId(),
                    waiting);

            LOGGER.info(
                    "ENPC-BACKPACK-DIALOGUE-PENDING "
                            + "operation={} session={} command={} "
                            + "replaced={} pendingCount={}",
                    operation,
                    sessionId,
                    result.commandId().value(),
                    previous != null,
                    pending.size());

            if (previous != null) {
                LOGGER.warn(
                        "ENPC-BACKPACK-DIALOGUE-PENDING-REPLACED "
                                + "command={} previousSession={} "
                                + "newSession={}",
                        result.commandId().value(),
                        previous.sessionId().value(),
                        sessionId);
            }
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "ENPC-BACKPACK-DIALOGUE-DISPATCH-FAILED "
                            + "operation={} session={} revision={} "
                            + "waitKey={} owner={}",
                    operation,
                    sessionId,
                    session.revision(),
                    wait.waitKey(),
                    owner,
                    exception);

            service.cancel(session.sessionId());
        }
    }

    public void tick(MinecraftServer server) {
        if (pending.isEmpty()) {
            return;
        }

        NpcBackpackScriptFacade facade =
                new NpcBackpackScriptFacade(server);

        for (Map.Entry<NpcBackpackCommandId, Pending> entry
                : pending.entrySet().stream().toList()) {

            Pending waiting = entry.getValue();

            if (waiting.server() != server) {
                continue;
            }

            NpcBackpackCommandId commandId = entry.getKey();

            try {
                facade.command(commandId.value())
                        .filter(NpcBackpackCommandResult::terminal)
                        .ifPresent(result -> {
                            LOGGER.info(
                                    "ENPC-BACKPACK-DIALOGUE-TERMINAL "
                                            + "operation={} session={} "
                                            + "command={} outcome={}",
                                    waiting.operation(),
                                    waiting.sessionId().value(),
                                    commandId.value(),
                                    result.outcome());

                            finish(
                                    commandId,
                                    result.outcome(),
                                    waiting);
                        });
            } catch (RuntimeException exception) {
                LOGGER.error(
                        "ENPC-BACKPACK-DIALOGUE-TICK-FAILED "
                                + "operation={} session={} command={} "
                                + "pendingCount={}",
                        waiting.operation(),
                        waiting.sessionId().value(),
                        commandId.value(),
                        pending.size(),
                        exception);
            }
        }
    }

    public void close(MinecraftServer server) {
        int before = pending.size();

        pending.entrySet().removeIf(
                entry -> entry.getValue().server() == server);

        int removed = before - pending.size();

        LOGGER.info(
                "ENPC-BACKPACK-DIALOGUE-CLOSE "
                        + "removed={} remaining={}",
                removed,
                pending.size());
    }

    private void finish(
            NpcBackpackCommandId commandId,
            NpcBackpackOutcome outcome,
            Pending waiting) {

        LOGGER.info(
                "ENPC-BACKPACK-DIALOGUE-FINISH "
                        + "operation={} session={} command={} "
                        + "outcome={} pendingBefore={}",
                waiting.operation(),
                waiting.sessionId().value(),
                commandId.value(),
                outcome,
                pending.size());

        complete(outcome, waiting);

        Pending removed = pending.remove(commandId);

        LOGGER.info(
                "ENPC-BACKPACK-DIALOGUE-REMOVED "
                        + "operation={} session={} command={} "
                        + "removed={} pendingAfter={}",
                waiting.operation(),
                waiting.sessionId().value(),
                commandId.value(),
                removed != null,
                pending.size());
    }

    private void complete(
            NpcBackpackOutcome outcome,
            Pending waiting) {

        boolean successful =
                outcome == NpcBackpackOutcome.SUCCEEDED
                        || outcome
                        == NpcBackpackOutcome.ALREADY_SATISFIED;

        if (successful) {
            LOGGER.info(
                    "ENPC-BACKPACK-DIALOGUE-RESUME "
                            + "operation={} session={} revision={} "
                            + "waitKey={} outcome={}",
                    waiting.operation(),
                    waiting.sessionId().value(),
                    waiting.expectedRevision(),
                    waiting.waitKey(),
                    outcome);

            waitResume(waiting);
            return;
        }

        LOGGER.warn(
                "ENPC-BACKPACK-DIALOGUE-CANCEL "
                        + "operation={} session={} revision={} "
                        + "waitKey={} outcome={}",
                waiting.operation(),
                waiting.sessionId().value(),
                waiting.expectedRevision(),
                waiting.waitKey(),
                outcome);

        waiting.service().cancel(waiting.sessionId());
    }

    private void waitResume(Pending waiting) {
        waiting.service().resumeExternal(
                new DialogueExternalResumeRequest(
                        waiting.sessionId(),
                        waiting.expectedRevision(),
                        waiting.waitKey()));
    }

    private record Pending(
            MinecraftServer server,
            DialogueService service,
            DialogueSessionId sessionId,
            int expectedRevision,
            String waitKey,
            String operation) {
    }
}
