package io.github.kxyr0.echonpcnavigationadapter.dialogue;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.command.DialogueCommandPayload;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;
import io.github.kxyr0.echonavigation.forge.api.service.EchoNavigationServiceBridge;
import io.github.kxyr0.echonavigation.forge.api.service.NavigationQueryService;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcnavigationadapter.navigation.NpcSubjectIds;
import io.github.kxyr0.echonpcnavigationadapter.script.NpcNavigationPayloads;
import io.github.kxyr0.echonpcnavigationadapter.script.NpcNavigationScriptFacade;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

final class DialogueNavigationMoves {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final NpcNavigationScriptFacade facade = new NpcNavigationScriptFacade();
    private final Map<MoveKey, ActiveMove> moves = new LinkedHashMap<>();
    private final Map<NavigationMoveId, MoveKey> moveKeys = new LinkedHashMap<>();

    void move(MinecraftServer server, DialogueService service, DialogueSessionView session,
            DialogueExternalWait wait) {
        try {
            var command = NpcNavigationPayloads.move(DialogueCommandPayload.decode(wait.payload()));
            LOGGER.info("ENPC-NAV-DIALOGUE-MOVE session={} actor={} target={}:{}:{}:{}",
                    session.sessionId().value(), command.actorId().value(), command.destination().dimensionId(),
                    command.destination().blockX(), command.destination().blockY(), command.destination().blockZ());
            var result = facade.move(server, ownerExecutionId(session, wait), command);
            LOGGER.info("ENPC-NAV-DIALOGUE-MOVE-RESULT session={} status={} message={}",
                    session.sessionId().value(), result.status(), result.message());
            result.snapshot().ifPresentOrElse(snapshot -> {
                rememberMove(new MoveKey(session.sessionId(), command.actorId()),
                        new ActiveMove(server, session.sessionId(), snapshot.moveId(),
                                moveState(snapshot.status()), terminalStatus(snapshot.status()), Optional.empty()));
                LOGGER.info("ENPC-NAV-DIALOGUE-ACTIVE session={} move={} navStatus={}",
                        session.sessionId().value(), snapshot.moveId().value(), snapshot.status());
                resume(service, session.sessionId(), session.revision(), wait.waitKey());
            }, () -> {
                LOGGER.warn("ENPC-NAV-DIALOGUE-CANCEL-NO-SNAPSHOT session={} status={} message={}",
                        session.sessionId().value(), result.status(), result.message());
                service.cancel(session.sessionId());
            });
        } catch (RuntimeException exception) {
            LOGGER.warn("ENPC-NAV-DIALOGUE-MOVE-FAILED session={}", session.sessionId().value(), exception);
            service.cancel(session.sessionId());
        }
    }

    void moveArrival(MinecraftServer server, DialogueService service, DialogueSessionView session,
            DialogueExternalWait wait) {
        try {
            ActorId actor = NpcNavigationPayloads.actor(DialogueCommandPayload.decode(wait.payload()));
            MoveKey key = new MoveKey(session.sessionId(), actor);
            ActiveMove move = moves.get(key);
            if (move == null) {
                move = adoptActiveMove(server, session.sessionId(), actor, key).orElse(null);
            }
            if (move == null) {
                LOGGER.warn("ENPC-NAV-DIALOGUE-ARRIVAL-NO-MOVE session={} actor={}",
                        session.sessionId().value(), actor.value());
                service.cancel(session.sessionId());
                return;
            }
            handleArrival(service, session, wait, key, actor, move);
        } catch (RuntimeException exception) {
            LOGGER.warn("ENPC-NAV-DIALOGUE-ARRIVAL-FAILED session={}", session.sessionId().value(), exception);
            service.cancel(session.sessionId());
        }
    }

    void stop(MinecraftServer server, DialogueService service, DialogueSessionView session, DialogueExternalWait wait) {
        try {
            ActorId actor = NpcNavigationPayloads.actor(DialogueCommandPayload.decode(wait.payload()));
            facade.stop(server, actor);
            markMoveFailed(new MoveKey(session.sessionId(), actor), NavigationStatus.CANCELLED);
            LOGGER.info("ENPC-NAV-DIALOGUE-STOP session={}", session.sessionId().value());
            resume(service, session.sessionId(), session.revision(), wait.waitKey());
        } catch (RuntimeException exception) {
            LOGGER.warn("ENPC-NAV-DIALOGUE-STOP-FAILED session={}", session.sessionId().value(), exception);
            service.cancel(session.sessionId());
        }
    }

    void tick(MinecraftServer server, NavigationQueryService service) {
        for (Map.Entry<NavigationMoveId, MoveKey> entry : moveKeys.entrySet().stream().toList()) {
            ActiveMove move = moves.get(entry.getValue());
            if (move != null && move.server() == server) {
                service.move(entry.getKey()).filter(snapshot -> snapshot.status().terminal())
                        .ifPresent(snapshot -> complete(entry.getKey(), entry.getValue(), move, snapshot));
            }
        }
    }

    void close(MinecraftServer server) {
        moves.entrySet().removeIf(entry -> entry.getValue().server() == server);
        moveKeys.entrySet().removeIf(entry -> !moves.containsKey(entry.getValue()));
    }

    private Optional<ActiveMove> adoptActiveMove(MinecraftServer server, DialogueSessionId sessionId,
            ActorId actor, MoveKey key) {
        return EchoNavigationServiceBridge.lookup(server).queryService()
                .flatMap(query -> query.active(NpcSubjectIds.subject(actor)))
                .map(snapshot -> {
                    ActiveMove move = new ActiveMove(server, sessionId, snapshot.moveId(),
                            moveState(snapshot.status()), terminalStatus(snapshot.status()), Optional.empty());
                    rememberMove(key, move);
                    LOGGER.info("ENPC-NAV-DIALOGUE-ARRIVAL-ADOPT-ACTIVE session={} actor={} move={} status={}",
                            sessionId.value(), actor.value(), snapshot.moveId().value(), snapshot.status());
                    return move;
                });
    }

    private void handleArrival(DialogueService service, DialogueSessionView session, DialogueExternalWait wait,
            MoveKey key, ActorId actor, ActiveMove move) {
        if (move.state() == MoveState.ARRIVED) {
            LOGGER.info("ENPC-NAV-DIALOGUE-ARRIVAL-READY session={} actor={} move={}",
                    session.sessionId().value(), actor.value(), move.moveId().value());
            forgetMove(key, move);
            resume(service, session.sessionId(), session.revision(), wait.waitKey());
            return;
        }
        if (move.state() == MoveState.FAILED) {
            LOGGER.warn("ENPC-NAV-DIALOGUE-ARRIVAL-FAILED session={} actor={} move={} status={}",
                    session.sessionId().value(), actor.value(), move.moveId().value(),
                    move.terminalStatus().map(Enum::name).orElse(""));
            forgetMove(key, move);
            service.cancel(session.sessionId());
            return;
        }
        moves.put(key, move.withWaiter(
                new Pending(service, session.sessionId(), session.revision(), wait.waitKey())));
        LOGGER.info("ENPC-NAV-DIALOGUE-ARRIVAL-PENDING session={} actor={} move={}",
                session.sessionId().value(), actor.value(), move.moveId().value());
    }

    private void complete(NavigationMoveId moveId, MoveKey key, ActiveMove move, NavigationSnapshot snapshot) {
        LOGGER.info("ENPC-NAV-DIALOGUE-COMPLETE session={} move={} status={} outcome={} failure={}",
                move.sessionId().value(), moveId.value(), snapshot.status(),
                snapshot.outcome().map(Enum::name).orElse(""), snapshot.failure().map(Enum::name).orElse(""));
        ActiveMove completed = move.withCompletion(moveState(snapshot.status()), snapshot.status());
        if (completed.waiter().isPresent()) {
            Pending waiter = completed.waiter().orElseThrow();
            if (completed.state() == MoveState.ARRIVED) {
                resume(waiter.service(), waiter.sessionId(), waiter.expectedRevision(), waiter.waitKey());
            } else {
                waiter.service().cancel(waiter.sessionId());
            }
            forgetMove(key, completed);
            return;
        }
        moves.put(key, completed);
        moveKeys.remove(moveId);
    }

    private void rememberMove(MoveKey key, ActiveMove move) {
        ActiveMove previous = moves.put(key, move);
        if (previous != null) {
            moveKeys.remove(previous.moveId());
        }
        if (move.state() == MoveState.RUNNING) {
            moveKeys.put(move.moveId(), key);
        }
    }

    private void forgetMove(MoveKey key, ActiveMove move) {
        moves.remove(key);
        moveKeys.remove(move.moveId());
    }

    private void markMoveFailed(MoveKey key, NavigationStatus status) {
        ActiveMove move = moves.get(key);
        if (move == null) {
            return;
        }
        ActiveMove failed = move.withCompletion(MoveState.FAILED, status);
        if (failed.waiter().isPresent()) {
            Pending waiter = failed.waiter().orElseThrow();
            waiter.service().cancel(waiter.sessionId());
            forgetMove(key, failed);
            return;
        }
        moves.put(key, failed);
        moveKeys.remove(move.moveId());
    }

    private MoveState moveState(NavigationStatus status) {
        if (status == NavigationStatus.ARRIVED) {
            return MoveState.ARRIVED;
        }
        return status.terminal() ? MoveState.FAILED : MoveState.RUNNING;
    }

    private Optional<NavigationStatus> terminalStatus(NavigationStatus status) {
        return status.terminal() ? Optional.of(status) : Optional.empty();
    }

    private void resume(DialogueService service, DialogueSessionId sessionId, int expectedRevision, String waitKey) {
        service.resumeExternal(new DialogueExternalResumeRequest(sessionId, expectedRevision, waitKey));
    }

    private String ownerExecutionId(DialogueSessionView session, DialogueExternalWait wait) {
        return "dialogue_" + session.sessionId().value() + "_" + wait.waitKey();
    }

}
