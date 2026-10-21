package io.github.kxyr0.echostoryengine.story.echoscript.diagnostic;

import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.diagnostic.LanguageDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts EchoScript language diagnostics into Story source diagnostics without exposing EchoScript types.
 */
public final class EchoScriptDiagnosticAdapter {
    public List<StoryDiagnostic> adapt(ParseResult result, Instant occurredAt) {
        return result.diagnostics().stream()
                .map(diagnostic -> adapt(diagnostic, occurredAt))
                .toList();
    }

    private StoryDiagnostic adapt(LanguageDiagnostic diagnostic, Instant occurredAt) {
        Map<String, String> context = context(diagnostic);
        String location = diagnostic.span()
                .map(span -> span.start().line() + ":" + span.start().column())
                .orElse("");
        return new StoryDiagnostic(
                storyCode(diagnostic),
                severity(diagnostic),
                "source",
                diagnostic.message(),
                new DiagnosticSubject("", "", "", "", diagnostic.sourceId().value(), location),
                context,
                cause(diagnostic),
                occurredAt,
                diagnostic.hint().orElse(""));
    }

    private Map<String, String> context(LanguageDiagnostic diagnostic) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("sourceId", diagnostic.sourceId().value());
        values.put("languageCode", diagnostic.code());
        diagnostic.span().ifPresent(span -> {
            values.put("line", Integer.toString(span.start().line()));
            values.put("column", Integer.toString(span.start().column()));
        });
        if (!diagnostic.context().isBlank()) {
            values.put("languageContext", diagnostic.context());
        }
        return Map.copyOf(values);
    }

    private String storyCode(LanguageDiagnostic diagnostic) {
        return diagnostic.code().startsWith("ESL-LANGUAGE-") ? "ESE-SOURCE-002" : "ESE-SOURCE-001";
    }

    private String cause(LanguageDiagnostic diagnostic) {
        return diagnostic.code().startsWith("ESL-LANGUAGE-") ? "source-version" : "source-syntax";
    }

    private DiagnosticSeverity severity(LanguageDiagnostic diagnostic) {
        return diagnostic.severity() == io.github.kxyr0.echoscript.api.diagnostic.DiagnosticSeverity.WARNING
                ? DiagnosticSeverity.WARN
                : DiagnosticSeverity.ERROR;
    }
}
