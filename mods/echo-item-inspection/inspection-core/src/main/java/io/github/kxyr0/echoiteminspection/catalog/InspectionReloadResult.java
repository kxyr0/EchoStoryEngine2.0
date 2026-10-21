package io.github.kxyr0.echoiteminspection.catalog;

import io.github.kxyr0.echoiteminspection.diagnostic.InspectionDiagnostic;
import java.util.List;

public record InspectionReloadResult(
        boolean applied, InspectionCatalogSnapshot snapshot, List<InspectionDiagnostic> diagnostics
) {
    public InspectionReloadResult {
        diagnostics = List.copyOf(diagnostics);
    }
}
