package io.github.kxyr0.echoquests.api.condition;

@FunctionalInterface
public interface QuestConditionResolver {
    boolean test(QuestCondition condition);
}
