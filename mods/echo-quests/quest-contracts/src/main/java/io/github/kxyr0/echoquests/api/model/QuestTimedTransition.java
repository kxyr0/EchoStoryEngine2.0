package io.github.kxyr0.echoquests.api.model;

import io.github.kxyr0.echoquests.api.condition.QuestCondition;
import io.github.kxyr0.echoquests.api.identity.QuestStateId;
import java.util.Objects;

public record QuestTimedTransition(
        String time,
        QuestCondition condition,
        QuestStateId satisfiedState,
        QuestStateId otherwiseState) {
    public QuestTimedTransition {
        time = Objects.requireNonNull(time, "time");
        condition = Objects.requireNonNull(condition, "condition");
        satisfiedState = Objects.requireNonNull(satisfiedState, "satisfiedState");
        otherwiseState = Objects.requireNonNull(otherwiseState, "otherwiseState");
    }
}
