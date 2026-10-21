package io.github.kxyr0.echodevscenarios.definition;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echodevscenarios.api.ScenarioId;
import io.github.kxyr0.echodevscenarios.api.ScenarioTarget;
import java.util.List;
import java.util.Objects;

public record ScenarioDefinition(
        ScenarioId id,
        String title,
        List<String> groups,
        List<ScenarioSetupStep> setup,
        ScenarioTarget target,
        SourceSpan span
) {
    public ScenarioDefinition {
        id = Objects.requireNonNull(id, "id");
        title = Objects.requireNonNull(title, "title");
        groups = List.copyOf(groups);
        setup = List.copyOf(setup);
        target = Objects.requireNonNull(target, "target");
        span = Objects.requireNonNull(span, "span");
    }
}
