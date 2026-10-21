package io.github.kxyr0.echonpcinteractionnavigationadapter.navigation;

import io.github.kxyr0.echonavigation.api.identity.NavigationCommandId;
import io.github.kxyr0.echonavigation.api.identity.NavigationExecutionId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationOwnerId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId;

final class NpcInteractionNavigationIds {
    private NpcInteractionNavigationIds() {
    }

    static NavigationMoveId move(NpcInteractionCommandId commandId) {
        return new NavigationMoveId("echo_npc_interaction:" + commandId.value().replace(':', '_'));
    }

    static NavigationCommandId command(NpcInteractionCommandId commandId) {
        return new NavigationCommandId("echo_npc_interaction:navigation_" + commandId.value().replace(':', '_'));
    }

    static NavigationExecutionId execution(NpcInteractionCommandId commandId) {
        return new NavigationExecutionId("echo_npc_interaction:" + commandId.value().replace(':', '_'));
    }

    static NavigationOwnerId owner(NpcInteractionCommandId commandId) {
        return new NavigationOwnerId("echo_npc_interaction:" + commandId.value().replace(':', '_'));
    }

    static NavigationSubjectId subject(ActorId actorId) {
        return new NavigationSubjectId(actorId.value());
    }
}
