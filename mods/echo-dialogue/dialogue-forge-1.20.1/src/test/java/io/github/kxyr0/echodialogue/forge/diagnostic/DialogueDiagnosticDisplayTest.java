package io.github.kxyr0.echodialogue.forge.diagnostic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import org.junit.jupiter.api.Test;

class DialogueDiagnosticDisplayTest {
    @Test
    void errorIncludesDatapackPathCoordinatesAndRussianMessage() {
        DialogueDiagnostic diagnostic = new DialogueDiagnostic(
                "EDLG-SYNTAX-ESL-PARSE-003",
                DiagnosticSeverity.ERROR,
                DiagnosticCategory.SYNTAX_ADAPTATION,
                "Missing semicolon.",
                DiagnosticSubject.source("jdh:echo_dialogue/dialogues/chapter_1/home_arrival.dialogue.es", 12, 5));

        String line = new DialogueDiagnosticDisplay().error(diagnostic);

        assertTrue(line.contains("data/jdh/echo_dialogue/dialogues/chapter_1/home_arrival.dialogue.es"));
        assertTrue(line.contains("Строка: 12"));
        assertTrue(line.contains("Колонка: 5"));
        assertTrue(line.contains("Пропущена точка с запятой"));
    }
}
