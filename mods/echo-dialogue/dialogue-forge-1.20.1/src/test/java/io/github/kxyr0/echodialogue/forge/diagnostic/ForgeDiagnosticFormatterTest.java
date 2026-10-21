package io.github.kxyr0.echodialogue.forge.diagnostic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import java.util.List;
import org.junit.jupiter.api.Test;

class ForgeDiagnosticFormatterTest {
    @Test
    void diagnosticsIncludeLogicalSourceAndCoordinates() {
        ForgeDiagnosticFormatter formatter = new ForgeDiagnosticFormatter();
        DialogueDiagnostic diagnostic = new DialogueDiagnostic(
                "EDLG-SEM-011",
                DiagnosticSeverity.ERROR,
                DiagnosticCategory.SEMANTIC_VALIDATION,
                "Line ссылается на неизвестный speaker alias: ghost",
                DiagnosticSubject.source("sample:broken.dialogue.es", 25, 9));

        List<String> lines = formatter.diagnostics(List.of(diagnostic), 10);

        assertTrue(lines.get(1).contains("Файл: data/sample/broken.dialogue.es"));
        assertTrue(lines.get(1).contains("Строка: 25"));
        assertTrue(lines.get(1).contains("Колонка: 9"));
        assertTrue(lines.get(1).contains("EDLG-SEM-011"));
    }
}
