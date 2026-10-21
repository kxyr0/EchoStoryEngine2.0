package io.github.kxyr0.echoquests.core.catalog;

import io.github.kxyr0.echoquests.api.model.QuestDefinition;
import io.github.kxyr0.echoquests.api.model.QuestStateDefinition;
import io.github.kxyr0.echoquests.api.model.QuestStateKind;
import io.github.kxyr0.echoquests.core.condition.QuestConditions;
import java.util.HashSet;
import java.util.regex.Pattern;

final class QuestDefinitionValidator {
    private static final Pattern TIME = Pattern.compile("(?:[01]\\d|2[0-3]):[0-5]\\d");
    private QuestDefinitionValidator() { }

    static void validate(QuestDefinition definition) {
        if (definition.title().isBlank()) throw failure(definition, "title is blank");
        QuestStateDefinition initial = definition.states().get(definition.initialState());
        if (initial == null) throw failure(definition, "initial state is missing: " + definition.initialState());
        if (initial.kind() != QuestStateKind.ACTIVE) throw failure(definition, "initial state must be active");
        if (definition.states().isEmpty()) throw failure(definition, "no states declared");
        for (QuestStateDefinition state : definition.states().values()) validate(definition, state);
    }

    private static void validate(QuestDefinition definition, QuestStateDefinition state) {
        if (state.kind() == QuestStateKind.ACTIVE && state.objective().isBlank()) {
            throw failure(definition, "active state has no objective: " + state.id());
        }
        if (state.kind() != QuestStateKind.ACTIVE && !state.transitions().isEmpty()) {
            throw failure(definition, "terminal state has transitions: " + state.id());
        }
        if (state.kind() != QuestStateKind.ACTIVE && !state.timedTransitions().isEmpty()) {
            throw failure(definition, "terminal state has timed transitions: " + state.id());
        }
        state.transitions().forEach((event, target) -> {
            if (!definition.states().containsKey(target)) {
                throw failure(definition, "transition " + event + " targets missing state " + target);
            }
        });
        var times = new HashSet<String>();
        state.timedTransitions().forEach(value -> {
            if (!TIME.matcher(value.time()).matches()) {
                throw failure(definition, "time must use strict HH:mm: " + value.time());
            }
            if (!times.add(value.time())) {
                throw failure(definition, "duplicate timed transition in " + state.id() + " at " + value.time());
            }
            if (!definition.states().containsKey(value.satisfiedState())) {
                throw failure(definition, "timed transition targets missing state " + value.satisfiedState());
            }
            if (!definition.states().containsKey(value.otherwiseState())) {
                throw failure(definition, "timed fallback targets missing state " + value.otherwiseState());
            }
            QuestConditions.validate(value.condition());
        });
    }

    private static IllegalArgumentException failure(QuestDefinition definition, String message) {
        return new IllegalArgumentException("Quest " + definition.id() + ": " + message);
    }
}
