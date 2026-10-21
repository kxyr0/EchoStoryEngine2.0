package io.github.kxyr0.echosavemigration.graph;

import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import io.github.kxyr0.echosavemigration.definition.MigrationDefinition;
import java.util.List;

public record MigrationPlan(
        SaveSchemaVersion source,
        SaveSchemaVersion target,
        List<MigrationDefinition> steps
) {
    public MigrationPlan {
        steps = List.copyOf(steps);
    }

    public int operationCount() {
        return steps.stream().mapToInt(step -> step.operations().size()).sum();
    }
}
