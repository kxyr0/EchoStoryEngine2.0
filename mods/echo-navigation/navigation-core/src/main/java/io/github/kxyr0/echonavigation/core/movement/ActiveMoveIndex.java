package io.github.kxyr0.echonavigation.core.movement;

import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class ActiveMoveIndex {
    private final Map<NavigationSubjectId, NavigationMoveId> activeBySubject = new HashMap<>();

    Optional<NavigationMoveId> active(NavigationSubjectId subjectId) {
        return Optional.ofNullable(activeBySubject.get(subjectId));
    }

    void put(NavigationSubjectId subjectId, NavigationMoveId moveId) {
        activeBySubject.put(subjectId, moveId);
    }

    void remove(NavigationSubjectId subjectId, NavigationMoveId moveId) {
        if (moveId.equals(activeBySubject.get(subjectId))) {
            activeBySubject.remove(subjectId);
        }
    }
}
