package io.github.kxyr0.echoiteminspection.compile;

import io.github.kxyr0.echoiteminspection.diagnostic.InspectionDiagnostic;
import io.github.kxyr0.echoscript.api.source.SourceSpan;

/** Expected authoring error with its original syntax location. */
final class BindingFailure extends RuntimeException {
    private final InspectionDiagnostic diagnostic;

    BindingFailure(String code, String message, SourceSpan span) {
        super(message);
        diagnostic = InspectionDiagnostic.error(code, message, span);
    }

    InspectionDiagnostic diagnostic() { return diagnostic; }
}
