package io.github.kxyr0.echodialogue.echoscript.api;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSeverity;
import java.util.Objects;

/** Stable extension diagnostic; the host attaches the source position. */
public record DialogueLineBindingIssue(String code, DiagnosticSeverity severity, String message) {
    public DialogueLineBindingIssue {
        code = requireText(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = requireText(message, "message");
    }

    public static DialogueLineBindingIssue error(String code, String message) {
        return new DialogueLineBindingIssue(code, DiagnosticSeverity.ERROR, message);
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }
}
