package io.github.kxyr0.echoplayernavigationadapter.navigation;

import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echonavigation.api.identity.NavigationCommandId;
import io.github.kxyr0.echonavigation.api.identity.NavigationExecutionId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationOwnerId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;

public final class PlayerNavigationIds {
    private PlayerNavigationIds() {
    }

    public static NavigationSubjectId subject(PlayerActorId actorId) {
        return new NavigationSubjectId("echo_player_actor:" + actorId.value());
    }

    public static NavigationCommandId command(PlayerActorCommandId commandId) {
        return new NavigationCommandId("echo_player_actor:" + commandId.value());
    }

    public static NavigationMoveId move(PlayerActorCommandId commandId) {
        return new NavigationMoveId("echo_player_actor:" + commandId.value());
    }

    public static NavigationExecutionId execution(PlayerActorCommandId commandId) {
        return new NavigationExecutionId("echo_player_actor:" + commandId.value());
    }

    public static NavigationOwnerId owner(PlayerActorOwnerId ownerId) {
        return new NavigationOwnerId("echo_player_actor:" + ownerId.value().replace(':', '_'));
    }
}
