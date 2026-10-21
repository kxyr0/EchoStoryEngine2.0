package io.github.kxyr0.echocontentdoctor.source;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.graph.ContentCatalogSnapshot;
import io.github.kxyr0.echosavemigration.definition.MigrationDefinition;
import java.util.List;

public record SourceScanResult(ContentCatalogSnapshot catalog, List<ContentDiagnostic> diagnostics,
                               List<MigrationDefinition> migrations) {
    public SourceScanResult {
        diagnostics = List.copyOf(diagnostics);
        migrations = List.copyOf(migrations);
    }
}
