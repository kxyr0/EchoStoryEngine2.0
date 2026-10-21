package io.github.kxyr0.echoquests.api.model;

import io.github.kxyr0.echoquests.api.identity.QuestEventId;
import io.github.kxyr0.echoquests.api.identity.QuestStateId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;

public record QuestStateDefinition(
        QuestStateId id,
        String objective,
        QuestStateKind kind,
        Map<QuestEventId, QuestStateId> transitions,
        List<QuestTimedTransition> timedTransitions) {

    public QuestStateDefinition {
        id = Objects.requireNonNull(id, "id");
        objective = Objects.requireNonNull(objective, "objective");
        kind = Objects.requireNonNull(kind, "kind");
        transitions = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(transitions, "transitions")));
        timedTransitions = List.copyOf(Objects.requireNonNull(timedTransitions, "timedTransitions"));
    }
}
