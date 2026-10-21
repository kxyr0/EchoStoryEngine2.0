package io.github.kxyr0.echonpcnavigationadapter.dialogue;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

record MoveKey(DialogueSessionId sessionId, ActorId actorId) {
}

record ActiveMove(MinecraftServer server, DialogueSessionId sessionId, NavigationMoveId moveId,
        MoveState state, Optional<NavigationStatus> terminalStatus, Optional<Pending> waiter) {
    ActiveMove withWaiter(Pending nextWaiter) {
        return new ActiveMove(server, sessionId, moveId, state, terminalStatus, Optional.of(nextWaiter));
    }

    ActiveMove withCompletion(MoveState nextState, NavigationStatus status) {
        return new ActiveMove(server, sessionId, moveId, nextState, Optional.of(status), waiter);
    }
}

enum MoveState {
    RUNNING,
    ARRIVED,
    FAILED
}

record Pending(DialogueService service, DialogueSessionId sessionId, int expectedRevision, String waitKey) {
}
