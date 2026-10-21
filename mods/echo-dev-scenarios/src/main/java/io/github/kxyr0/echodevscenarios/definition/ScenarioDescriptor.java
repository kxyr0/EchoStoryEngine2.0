package io.github.kxyr0.echodevscenarios.definition;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echodevscenarios.api.ScenarioId;
import io.github.kxyr0.echodevscenarios.api.ScenarioTarget;
import java.util.List;

public record ScenarioDescriptor(
        ScenarioId id,
        String title,
        List<String> groups,
        ScenarioTarget target,
        List<String> setupReferences,
        SourceSpan span
) {
    public ScenarioDescriptor {
        groups = List.copyOf(groups);
        setupReferences = List.copyOf(setupReferences);
    }
}
