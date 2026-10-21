package io.github.kxyr0.echoiteminspection.compile;

import io.github.kxyr0.echoiteminspection.diagnostic.InspectionDiagnostic;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionDefinition;
import java.util.List;
import java.util.Optional;

public record InspectionCompileResult(
        Optional<ItemInspectionDefinition> definition, List<InspectionDiagnostic> diagnostics
) {
    public InspectionCompileResult {
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean successful() {
        return definition.isPresent() && diagnostics.stream().noneMatch(InspectionDiagnostic::error);
    }
}
