package io.github.kxyr0.echostoryengine.story.forge.diagnostic;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import java.time.Instant;
import java.util.Map;

public record ForgeDiagnosticParts(
        String code,
        DiagnosticSeverity severity,
        String subsystem,
        String message,
        DiagnosticSubject subject,
        Map<String, String> context,
        String causeCategory,
        Instant occurredAt,
        String operatorHint) {
}
