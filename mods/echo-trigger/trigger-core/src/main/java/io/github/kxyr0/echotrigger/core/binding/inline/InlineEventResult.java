package io.github.kxyr0.echotrigger.core.binding.inline;

import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition;
import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import java.util.List;

public record InlineEventResult(List<EventTriggerDefinition> definitions, List<TriggerDiagnostic> diagnostics) {
    public InlineEventResult {
        definitions = List.copyOf(definitions);
        diagnostics = List.copyOf(diagnostics);
    }
}
