package io.github.kxyr0.echoscript.api;

import io.github.kxyr0.echoscript.api.source.SourceId;
import java.util.Objects;

/**
 * Immutable parser input supplied by a consumer.
 *
 * <p>The source identity is logical and platform-neutral. Source text is a Java {@link String} containing the UTF-8
 * decoded contents of a `.es` file. Line and column reporting in diagnostics is one-based; offsets are zero-based.</p>
 */
public record ParseRequest(
        LanguageVersion languageVersion,
        SourceId sourceId,
        String sourceText,
        ProcessingLimits limits
) {
    public ParseRequest {
        languageVersion = Objects.requireNonNull(languageVersion, "languageVersion");
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        sourceText = Objects.requireNonNull(sourceText, "sourceText");
        limits = Objects.requireNonNull(limits, "limits");
    }

    public static ParseRequest ofV1(SourceId sourceId, String sourceText) {
        return new ParseRequest(LanguageVersion.V1, sourceId, sourceText, ProcessingLimits.defaults());
    }
}
