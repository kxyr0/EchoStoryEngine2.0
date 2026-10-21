package io.github.kxyr0.echotrigger.api.model;

import io.github.kxyr0.echotrigger.api.event.TriggerEventKind;
import io.github.kxyr0.echotrigger.api.expression.TriggerExpressionKind;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record TriggerCondition(
        TriggerEventKind kind,
        Optional<TriggerArea> area,
        List<TriggerCondition> children,
        TriggerExpressionKind expressionKind,
        String expressionName,
        Map<String, String> arguments
) {
    public TriggerCondition {
        kind = Objects.requireNonNull(kind, "kind");
        area = Objects.requireNonNull(area, "area");
        children = List.copyOf(Objects.requireNonNull(children, "children"));
        expressionKind = Objects.requireNonNull(expressionKind, "expressionKind");
        expressionName = Objects.requireNonNull(expressionName, "expressionName").trim();
        arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments"));
        if (expressionName.isEmpty()) {
            throw new IllegalArgumentException("expressionName must not be blank.");
        }
    }

    public TriggerCondition(
            TriggerEventKind kind,
            Optional<TriggerArea> area,
            List<TriggerCondition> children
    ) {
        this(kind, area, children, TriggerExpressionKind.EVENT, kind.name(), Map.of());
    }

    public static TriggerCondition event(TriggerEventKind kind, TriggerArea area) {
        return new TriggerCondition(kind, Optional.of(area), List.of());
    }

    public static TriggerCondition expression(
            TriggerExpressionKind expressionKind,
            TriggerEventKind kind,
            Optional<TriggerArea> area,
            String expressionName,
            Map<String, String> arguments
    ) {
        return new TriggerCondition(kind, area, List.of(), expressionKind, expressionName, arguments);
    }

    public static TriggerCondition composite(String expressionName, List<TriggerCondition> children) {
        return new TriggerCondition(TriggerEventKind.PREDICATE_TRUE, Optional.empty(), children,
                TriggerExpressionKind.PREDICATE, expressionName, Map.of());
    }
}
