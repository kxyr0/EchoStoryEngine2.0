package io.github.kxyr0.echoscript.language.lexer;

import io.github.kxyr0.echoscript.language.diagnostic.DiagnosticBag;
import io.github.kxyr0.echoscript.language.diagnostic.DiagnosticCodes;
import io.github.kxyr0.echoscript.language.source.SourceText;

final class TriviaSkipper {
    private TriviaSkipper() {
    }

    static int skip(SourceText source, int offset, DiagnosticBag diagnostics) {
        boolean advanced = true;
        int current = offset;
        while (advanced && current < source.length()) {
            int beforeWhitespace = current;
            current = skipWhitespace(source, current);
            advanced = current != beforeWhitespace;
            if (current < source.length() && source.charAt(current) == '/' && peek(source, current, 1) == '/') {
                current = skipLineComment(source, current);
                advanced = true;
            } else if (current < source.length() && source.charAt(current) == '/' && peek(source, current, 1) == '*') {
                current = skipBlockComment(source, current, diagnostics);
                advanced = true;
            }
        }
        return current;
    }

    private static int skipWhitespace(SourceText source, int offset) {
        int current = offset;
        while (current < source.length() && Character.isWhitespace(source.charAt(current))) {
            current++;
        }
        return current;
    }

    private static int skipLineComment(SourceText source, int offset) {
        int current = offset + 2;
        while (current < source.length() && source.charAt(current) != '\n' && source.charAt(current) != '\r') {
            current++;
        }
        return current;
    }

    private static int skipBlockComment(SourceText source, int offset, DiagnosticBag diagnostics) {
        int current = offset + 2;
        while (current < source.length()) {
            if (source.charAt(current) == '*' && peek(source, current, 1) == '/') {
                return current + 2;
            }
            current++;
        }
        diagnostics.report(
                DiagnosticCodes.UNTERMINATED_BLOCK_COMMENT,
                source.span(offset, current),
                "Unterminated block comment."
        );
        return current;
    }

    private static char peek(SourceText source, int offset, int lookahead) {
        int target = offset + lookahead;
        return target >= source.length() ? '\0' : source.charAt(target);
    }
}
