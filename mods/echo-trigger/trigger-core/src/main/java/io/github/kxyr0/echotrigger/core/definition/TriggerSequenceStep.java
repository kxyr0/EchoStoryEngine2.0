package io.github.kxyr0.echotrigger.core.definition;

import io.github.kxyr0.echotrigger.api.model.TriggerExpression;
import java.util.Objects;

public record TriggerSequenceStep(int index, TriggerExpression expression) {
    public TriggerSequenceStep {
        if (index < 0) {
            throw new IllegalArgumentException("Step index must not be negative.");
        }
        expression = Objects.requireNonNull(expression, "expression");
    }
}
