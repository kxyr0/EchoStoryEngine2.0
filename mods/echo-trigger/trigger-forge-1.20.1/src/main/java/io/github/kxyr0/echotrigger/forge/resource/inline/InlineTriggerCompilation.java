package io.github.kxyr0.echotrigger.forge.resource.inline;

import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaDiagnosticView;
import io.github.kxyr0.echotrigger.core.definition.CompiledTriggerDefinition;
import java.util.List;

public record InlineTriggerCompilation(boolean handled, List<CompiledTriggerDefinition> definitions,
        List<AreaDiagnosticView> diagnostics) {
    public InlineTriggerCompilation {
        definitions = List.copyOf(definitions);
        diagnostics = List.copyOf(diagnostics);
    }
}
