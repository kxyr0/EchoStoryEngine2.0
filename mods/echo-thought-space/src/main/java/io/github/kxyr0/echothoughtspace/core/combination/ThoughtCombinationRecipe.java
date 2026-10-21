package io.github.kxyr0.echothoughtspace.core.combination;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeLabel;
import java.util.Objects;

public record ThoughtCombinationRecipe(
        ThoughtCombinationKey inputs,
        ThoughtNodeId result,
        ThoughtNodeLabel label
) {
    public ThoughtCombinationRecipe {
        inputs = Objects.requireNonNull(inputs, "inputs");
        result = Objects.requireNonNull(result, "result");
        label = Objects.requireNonNull(label, "label");
        if (result.equals(inputs.first()) || result.equals(inputs.second())) {
            throw new IllegalArgumentException("Combination result must be a third thought.");
        }
    }
}
