package io.github.kxyr0.echomagic.core.catalog;

import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnostic;
import java.util.List;

public record SpellCatalogPublishResult(boolean published, long revision, List<MagicDiagnostic> diagnostics) {
    public SpellCatalogPublishResult { diagnostics = List.copyOf(diagnostics); }
}

