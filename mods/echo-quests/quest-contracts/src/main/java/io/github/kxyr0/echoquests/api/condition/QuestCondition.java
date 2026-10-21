package io.github.kxyr0.echoquests.api.condition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record QuestCondition(String type, Map<String, String> arguments, List<QuestCondition> children) {
    public QuestCondition {
        type = Objects.requireNonNull(type, "type").trim();
        if (type.isEmpty()) throw new IllegalArgumentException("Quest condition type cannot be blank.");
        arguments = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(arguments, "arguments")));
        children = List.copyOf(Objects.requireNonNull(children, "children"));
    }

    public static QuestCondition leaf(String type, Map<String, String> arguments) {
        return new QuestCondition(type, arguments, List.of());
    }
}
