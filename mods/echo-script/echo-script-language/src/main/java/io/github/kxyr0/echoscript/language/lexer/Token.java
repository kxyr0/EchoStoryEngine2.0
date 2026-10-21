package io.github.kxyr0.echoscript.language.lexer;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;

public record Token(TokenKind kind, String text, String value, SourceSpan span) {
    public Token {
        kind = Objects.requireNonNull(kind, "kind");
        text = Objects.requireNonNull(text, "text");
        value = Objects.requireNonNull(value, "value");
        span = Objects.requireNonNull(span, "span");
    }

    public boolean is(TokenKind expected) {
        return kind == expected;
    }
}
