package io.github.kxyr0.echoquests.core.condition;

import io.github.kxyr0.echoquests.api.condition.QuestCondition;
import io.github.kxyr0.echoquests.api.condition.QuestConditionResolver;

public final class QuestConditions {
    private QuestConditions() { }

    public static boolean test(QuestCondition condition, QuestConditionResolver resolver) {
        return switch (condition.type()) {
            case "all" -> condition.children().stream().allMatch(value -> test(value, resolver));
            case "any" -> condition.children().stream().anyMatch(value -> test(value, resolver));
            case "not" -> !test(condition.children().get(0), resolver);
            default -> resolver.test(condition);
        };
    }

    public static void validate(QuestCondition condition) {
        switch (condition.type()) {
            case "all", "any" -> {
                if (condition.children().isEmpty()) throw new IllegalArgumentException(
                        condition.type() + " condition requires children");
                condition.children().forEach(QuestConditions::validate);
            }
            case "not" -> {
                if (condition.children().size() != 1) throw new IllegalArgumentException(
                        "not condition requires exactly one child");
                validate(condition.children().get(0));
            }
            default -> {
                if (!condition.children().isEmpty()) throw new IllegalArgumentException(
                        "leaf condition cannot contain children: " + condition.type());
            }
        }
    }
}
