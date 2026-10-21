package io.github.kxyr0.echoitem.core.script;

import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDiagnosticCodes;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDiagnosticSeverity;
import io.github.kxyr0.echoscript.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoscript.api.diagnostic.LanguageDiagnostic;
import java.util.List;
import java.util.Optional;

/**
 * Maps syntax-only EchoScript diagnostics to item diagnostics.
 */
public final class EchoScriptDiagnosticAdapter {
    public List<ItemDefinitionDiagnostic> adapt(List<LanguageDiagnostic> diagnostics) {
        return diagnostics.stream().map(this::adapt).toList();
    }

    private ItemDefinitionDiagnostic adapt(LanguageDiagnostic diagnostic) {
        ItemDiagnosticSeverity severity = diagnostic.severity() == DiagnosticSeverity.ERROR
                ? ItemDiagnosticSeverity.ERROR
                : ItemDiagnosticSeverity.WARNING;
        return new ItemDefinitionDiagnostic(
                ItemDiagnosticCodes.ITEM_SCRIPT_PARSE_ERROR,
                severity,
                diagnostic.code() + ": " + diagnostic.message(),
                Optional.empty(),
                diagnostic.sourceId().value()
        );
    }
}
