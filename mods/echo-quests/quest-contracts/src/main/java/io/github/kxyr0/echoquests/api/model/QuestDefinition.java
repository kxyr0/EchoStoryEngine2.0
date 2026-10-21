package io.github.kxyr0.echoquests.api.model;

import io.github.kxyr0.echoquests.api.identity.QuestId;
import io.github.kxyr0.echoquests.api.identity.QuestStateId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record QuestDefinition(
        QuestId id,
        String title,
        QuestStateId initialState,
        Map<QuestStateId, QuestStateDefinition> states) {

    public QuestDefinition {
        id = Objects.requireNonNull(id, "id");
        title = Objects.requireNonNull(title, "title");
        initialState = Objects.requireNonNull(initialState, "initialState");
        states = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(states, "states")));
    }
}
