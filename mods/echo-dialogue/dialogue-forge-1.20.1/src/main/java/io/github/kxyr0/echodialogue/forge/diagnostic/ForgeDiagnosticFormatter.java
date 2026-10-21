package io.github.kxyr0.echodialogue.forge.diagnostic;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.forge.catalog.CompiledDialogueCatalogSnapshot;
import java.util.ArrayList;
import java.util.List;

/**
 * Russian command-line formatting for bounded diagnostics and catalog status.
 */
public final class ForgeDiagnosticFormatter {
    private final DialogueDiagnosticDisplay display = new DialogueDiagnosticDisplay();

    public List<String> health(CompiledDialogueCatalogSnapshot catalog, int activeSessions) {
        return List.of(
                "Echo Dialogue: серверный host активен.",
                "Каталог revision: " + catalog.revision(),
                "Диалогов в каталоге: " + catalog.dialogueCount(),
                "Активных сессий: " + activeSessions);
    }

    public List<String> diagnostics(List<DialogueDiagnostic> diagnostics, int limit) {
        List<DialogueDiagnostic> tail = diagnostics.stream()
                .skip(Math.max(0, diagnostics.size() - limit))
                .toList();
        List<String> lines = new ArrayList<>();
        lines.add("Последние диагностики: " + tail.size());
        tail.forEach(diagnostic -> lines.add(display.command(diagnostic)));
        return lines;
    }
}
