package io.github.kxyr0.echoscript.api;

import io.github.kxyr0.echoscript.api.diagnostic.LanguageDiagnostic;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable parser output containing diagnostics and, when syntax succeeds, the compilation unit tree.
 *
 * <p>The diagnostics list is defensively copied and cannot be mutated by consumers. A failed parse never returns
 * executable behavior; EchoScript only returns source data and immutable syntax data.</p>
 */
public record ParseResult(
        LanguageVersion languageVersion,
        SourceId sourceId,
        Optional<CompilationUnitSyntax> syntaxTree,
        List<LanguageDiagnostic> diagnostics,
        boolean successful
) {
    public ParseResult {
        languageVersion = Objects.requireNonNull(languageVersion, "languageVersion");
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        syntaxTree = Objects.requireNonNull(syntaxTree, "syntaxTree");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (successful && syntaxTree.isEmpty()) {
            throw new IllegalArgumentException("Successful parse results require a syntax tree.");
        }
    }
}
