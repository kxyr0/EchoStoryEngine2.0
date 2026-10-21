package io.github.kxyr0.echoscript.language.lexer;

import io.github.kxyr0.echoscript.api.ProcessingLimits;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.language.diagnostic.DiagnosticBag;
import io.github.kxyr0.echoscript.language.diagnostic.DiagnosticCodes;
import io.github.kxyr0.echoscript.language.source.SourceText;
import java.util.ArrayList;
import java.util.List;

public final class EchoScriptLexer {
    private final SourceText source;
    private final ProcessingLimits limits;
    private final DiagnosticBag diagnostics;
    private final List<Token> tokens = new ArrayList<>();
    private int offset;
    private boolean halted;

    public EchoScriptLexer(SourceText source, ProcessingLimits limits) {
        this.source = source;
        this.limits = limits;
        this.diagnostics = new DiagnosticBag(source, limits);
    }

    public LexResult lex() {
        while (!isAtEnd() && !diagnostics.halted()) {
            int before = offset;
            offset = TriviaSkipper.skip(source, offset, diagnostics);
            if (isAtEnd() || diagnostics.halted()) {
                break;
            }
            addNextToken();
            if (offset == before) {
                reportUnexpectedAndAdvance();
            }
        }
        addToken(TokenKind.EOF, source.length(), source.length(), "");
        return new LexResult(tokens, diagnostics.diagnostics(), halted || diagnostics.halted());
    }

    private void addNextToken() {
        if (tokens.size() >= limits.maxTokenCount() - 1) {
            diagnostics.report(DiagnosticCodes.TOKEN_LIMIT_EXCEEDED, currentSpan(), "Token limit exceeded.");
            offset = source.length();
            halted = true;
            return;
        }
        char ch = source.charAt(offset);
        if (isIdentifierStart(ch)) {
            scanIdentifier();
        } else if (Character.isDigit(ch) || ch == '-' && offset + 1 < source.length()
                && Character.isDigit(source.charAt(offset + 1))) {
            scanNumber();
        } else if (ch == '"') {
            scanString();
        } else {
            scanPunctuation(ch);
        }
    }

    private void scanPunctuation(char ch) {
        int start = offset++;
        switch (ch) {
            case '{' -> addToken(TokenKind.OPEN_BRACE, start, offset, "");
            case '}' -> addToken(TokenKind.CLOSE_BRACE, start, offset, "");
            case '(' -> addToken(TokenKind.OPEN_PAREN, start, offset, "");
            case ')' -> addToken(TokenKind.CLOSE_PAREN, start, offset, "");
            case '[' -> addToken(TokenKind.OPEN_BRACKET, start, offset, "");
            case ']' -> addToken(TokenKind.CLOSE_BRACKET, start, offset, "");
            case ',' -> addToken(TokenKind.COMMA, start, offset, "");
            case '.' -> addToken(TokenKind.DOT, start, offset, "");
            case ':' -> addToken(TokenKind.COLON, start, offset, "");
            case ';' -> addToken(TokenKind.SEMICOLON, start, offset, "");
            case '=' -> addToken(TokenKind.EQUALS, start, offset, "");
            default -> diagnostics.report(
                    DiagnosticCodes.UNEXPECTED_CHARACTER,
                    source.span(start, offset),
                    "Unexpected character '" + ch + "'."
            );
        }
    }

    private void scanIdentifier() {
        int start = offset;
        offset++;
        while (!isAtEnd() && isIdentifierPart(source.charAt(offset))) {
            offset++;
        }
        String text = source.text().substring(start, offset);
        addToken(KeywordClassifier.kind(text), start, offset, text);
    }

    private void scanNumber() {
        int start = offset;
        if (source.charAt(offset) == '-') {
            offset++;
        }
        while (!isAtEnd() && Character.isDigit(source.charAt(offset))) {
            offset++;
        }
        if (hasDecimalFraction()) {
            offset++;
            while (!isAtEnd() && Character.isDigit(source.charAt(offset))) {
                offset++;
            }
            addToken(TokenKind.DECIMAL, start, offset, source.text().substring(start, offset));
            return;
        }
        addToken(TokenKind.INTEGER, start, offset, source.text().substring(start, offset));
    }

    private boolean hasDecimalFraction() {
        return offset + 1 < source.length()
                && source.charAt(offset) == '.'
                && Character.isDigit(source.charAt(offset + 1));
    }

    private void scanString() {
        int start = offset++;
        StringBuilder value = new StringBuilder();
        while (!isAtEnd()) {
            char ch = source.charAt(offset++);
            if (ch == '"') {
                addStringToken(start, value.toString());
                return;
            }
            if (ch == '\\') {
                appendEscape(start, value);
            } else if (ch == '\r' || ch == '\n') {
                diagnostics.report(
                        DiagnosticCodes.UNTERMINATED_STRING,
                        source.span(start, offset),
                        "Unterminated string."
                );
                return;
            } else {
                value.append(ch);
            }
        }
        diagnostics.report(DiagnosticCodes.UNTERMINATED_STRING, source.span(start, offset), "Unterminated string.");
    }

    private void appendEscape(int start, StringBuilder value) {
        if (isAtEnd()) {
            diagnostics.report(DiagnosticCodes.UNTERMINATED_STRING, source.span(start, offset), "Unterminated string.");
            return;
        }
        char escaped = source.charAt(offset++);
        switch (escaped) {
            case '"' -> value.append('"');
            case '\\' -> value.append('\\');
            case 'n' -> value.append('\n');
            case 'r' -> value.append('\r');
            case 't' -> value.append('\t');
            default -> diagnostics.report(
                    DiagnosticCodes.INVALID_STRING_ESCAPE,
                    source.span(offset - 2, offset),
                    "Invalid string escape '\\" + escaped + "'."
            );
        }
    }

    private void addStringToken(int start, String value) {
        if (value.length() > limits.maxStringLiteralLength()) {
            diagnostics.report(
                    DiagnosticCodes.STRING_LIMIT_EXCEEDED,
                    source.span(start, offset),
                    "String literal too long."
            );
            return;
        }
        addToken(TokenKind.STRING, start, offset, value);
    }

    private void reportUnexpectedAndAdvance() {
        int start = offset;
        offset++;
        diagnostics.report(DiagnosticCodes.UNEXPECTED_CHARACTER, source.span(start, offset), "Unexpected character.");
    }

    private void addToken(TokenKind kind, int start, int end, String value) {
        tokens.add(new Token(kind, source.text().substring(start, end), value, source.span(start, end)));
    }

    private SourceSpan currentSpan() { return source.span(offset, Math.min(offset + 1, source.length())); }

    private boolean isAtEnd() { return offset >= source.length(); }

    private static boolean isIdentifierStart(char ch) {
        return Character.isLetter(ch) || ch == '_';
    }

    private static boolean isIdentifierPart(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }
}
