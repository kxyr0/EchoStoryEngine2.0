package io.github.kxyr0.echonavigation.api.diagnostic;

import java.util.Map;
import java.util.Objects;

public record NavigationDiagnostic(
        String code,
        NavigationDiagnosticSeverity severity,
        String message,
        Map<String, String> context) {
    public NavigationDiagnostic {
        code = require(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = require(message, "message");
        context = Map.copyOf(Objects.requireNonNull(context, "context"));
    }

    public static NavigationDiagnostic error(String code, String message, Map<String, String> context) {
        return new NavigationDiagnostic(code, NavigationDiagnosticSeverity.ERROR, message, context);
    }

    public static NavigationDiagnostic info(String code, String message, Map<String, String> context) {
        return new NavigationDiagnostic(code, NavigationDiagnosticSeverity.INFO, message, context);
    }

    private static String require(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }
}
