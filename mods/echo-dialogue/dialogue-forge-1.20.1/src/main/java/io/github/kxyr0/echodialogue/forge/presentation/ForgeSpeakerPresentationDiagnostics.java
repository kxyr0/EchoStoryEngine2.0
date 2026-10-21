package io.github.kxyr0.echodialogue.forge.presentation;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;

/**
 * Stable bounded diagnostics for dynamic speaker presentation providers.
 */
final class ForgeSpeakerPresentationDiagnostics {
    private static final int MAX_TEXT = 160;

    private ForgeSpeakerPresentationDiagnostics() {
    }

    static DialogueDiagnostic info(String code, DiagnosticCategory category, String message) {
        return new DialogueDiagnostic(code, DiagnosticSeverity.INFO, category, bound(message),
                io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSubject.none());
    }

    static DialogueDiagnostic warning(String code, DiagnosticCategory category, String message) {
        return new DialogueDiagnostic(code, DiagnosticSeverity.WARNING, category, bound(message),
                io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSubject.none());
    }

    static DialogueDiagnostic error(String code, DiagnosticCategory category, String message) {
        return new DialogueDiagnostic(code, DiagnosticSeverity.ERROR, category, bound(message),
                io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSubject.none());
    }

    static String bound(String value) {
        String checked = java.util.Objects.requireNonNull(value, "value").trim();
        if (checked.length() <= MAX_TEXT) {
            return checked;
        }
        return checked.substring(0, MAX_TEXT);
    }
}
