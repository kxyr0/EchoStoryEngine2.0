package io.github.kxyr0.echocontentdoctor.source;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import java.util.List;

public record ContentSourceProblem(
        ContentSeverity severity,
        String code,
        ContentResourceId resource,
        String sourceId,
        String sourceText,
        int line,
        int column,
        String message
) {
    public ContentDiagnostic diagnostic() {
        return new ContentDiagnostic(severity, code, resource,
                SourceSpans.at(sourceId, sourceText, line, column), message, List.of());
    }
}
