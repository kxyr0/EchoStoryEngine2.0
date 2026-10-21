package io.github.kxyr0.echonpcnavigationadapter.navigation;

import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonpc.api.identity.ActorId;

public final class NpcSubjectIds {
    private NpcSubjectIds() {
    }

    public static NavigationSubjectId subject(ActorId actorId) {
        return new NavigationSubjectId(actorId.value());
    }

    public static ActorId actor(NavigationSubjectId subjectId) {
        return new ActorId(subjectId.value());
    }
}
