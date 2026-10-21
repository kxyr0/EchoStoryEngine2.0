package io.github.kxyr0.echoscript.language.lexer;

import io.github.kxyr0.echoscript.api.diagnostic.LanguageDiagnostic;
import java.util.List;
import java.util.Objects;

public record LexResult(List<Token> tokens, List<LanguageDiagnostic> diagnostics, boolean halted) {
    public LexResult {
        tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
