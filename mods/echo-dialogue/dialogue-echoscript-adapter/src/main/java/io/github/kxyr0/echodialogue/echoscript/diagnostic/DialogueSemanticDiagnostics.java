package io.github.kxyr0.echodialogue.echoscript.diagnostic;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echoscript.api.syntax.SyntaxNode;

/**
 * Factory for DialogueResource semantic diagnostics.
 */
public final class DialogueSemanticDiagnostics {
    private DialogueSemanticDiagnostics() {
    }

    public static DialogueDiagnostic semantic(String code, String message) {
        return DialogueDiagnostic.error(code, DiagnosticCategory.SEMANTIC_VALIDATION, message);
    }

    public static DialogueDiagnostic semantic(String code, String message, SyntaxNode node) {
        return new DialogueDiagnostic(code, DiagnosticSeverity.ERROR, DiagnosticCategory.SEMANTIC_VALIDATION,
                message, DiagnosticSubject.source(
                        node.span().sourceId().value(),
                        node.span().start().line(),
                        node.span().start().column()));
    }

    public static DialogueDiagnostic warning(String code, String message) {
        return DialogueDiagnostic.warning(code, DiagnosticCategory.SEMANTIC_VALIDATION, message);
    }

    public static DialogueDiagnostic warning(String code, String message, SyntaxNode node) {
        return new DialogueDiagnostic(code, DiagnosticSeverity.WARNING, DiagnosticCategory.SEMANTIC_VALIDATION,
                message, DiagnosticSubject.source(
                        node.span().sourceId().value(),
                        node.span().start().line(),
                        node.span().start().column()));
    }

    public static DialogueDiagnostic unsupported(String feature) {
        return DialogueDiagnostic.error(
                "EDLG-SEM-UNSUPPORTED",
                DiagnosticCategory.UNSUPPORTED_DIALOGUE_RESOURCE_FEATURE,
                "Профиль DialogueResource D1 не поддерживает: " + feature);
    }

    public static DialogueDiagnostic unsupported(String feature, SyntaxNode node) {
        return new DialogueDiagnostic(
                "EDLG-SEM-UNSUPPORTED",
                DiagnosticSeverity.ERROR,
                DiagnosticCategory.UNSUPPORTED_DIALOGUE_RESOURCE_FEATURE,
                "Профиль DialogueResource D1 не поддерживает: " + feature,
                DiagnosticSubject.source(
                        node.span().sourceId().value(),
                        node.span().start().line(),
                        node.span().start().column()));
    }
}
