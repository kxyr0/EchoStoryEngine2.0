package io.github.kxyr0.echoquests.echoscript;

import io.github.kxyr0.echoquests.api.diagnostic.QuestDiagnostic;
import io.github.kxyr0.echoquests.core.catalog.QuestCatalog;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record QuestCompileResult(Optional<QuestCatalog> catalog, List<QuestDiagnostic> diagnostics) {
    public QuestCompileResult {
        catalog = Objects.requireNonNull(catalog, "catalog");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean successful() { return catalog.isPresent() && diagnostics.stream().noneMatch(QuestDiagnostic::error); }
}
