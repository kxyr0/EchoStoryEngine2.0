package io.github.kxyr0.echoscript.language.parser;

import io.github.kxyr0.echoscript.api.ProcessingLimits;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.QualifiedNameSyntax;
import io.github.kxyr0.echoscript.api.syntax.SyntaxModifier;
import io.github.kxyr0.echoscript.api.syntax.declaration.TypeSyntax;
import io.github.kxyr0.echoscript.language.diagnostic.DiagnosticBag;
import io.github.kxyr0.echoscript.language.diagnostic.DiagnosticCodes;
import io.github.kxyr0.echoscript.language.lexer.Token;
import io.github.kxyr0.echoscript.language.lexer.TokenKind;
import io.github.kxyr0.echoscript.language.parser.DefaultEchoScriptParser.TokenCursor;
import io.github.kxyr0.echoscript.language.source.SourceText;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class TypeParser {
    private final TokenCursor cursor;
    private final ParseContext context;

    TypeParser(TokenCursor cursor, ParseContext context) {
        this.cursor = cursor;
        this.context = context;
    }

    List<SyntaxModifier> parseModifiers() {
        List<SyntaxModifier> modifiers = new ArrayList<>();
        while (cursor.current().kind().isModifier()) {
            modifiers.add(toModifier(cursor.consume().kind()));
        }
        return modifiers;
    }

    TypeSyntax parseType() {
        Token first = cursor.current();
        QualifiedNameSyntax name = parseTypeName();
        int rank = 0;
        Token last = cursor.previous();
        while (cursor.match(TokenKind.OPEN_BRACKET)) {
            if (!cursor.match(TokenKind.CLOSE_BRACKET)) {
                context.missing(cursor.current(), DiagnosticCodes.MISSING_CLOSING_BRACKET, "Missing closing bracket.");
            }
            rank++;
            last = cursor.previous();
        }
        return new TypeSyntax(name, rank, context.span(first, last));
    }

    QualifiedNameSyntax parseQualifiedName() {
        Token first = cursor.current();
        List<String> segments = new ArrayList<>();
        if (!cursor.check(TokenKind.IDENTIFIER)) {
            context.expected(cursor.current(), TokenKind.IDENTIFIER);
            return missingName(first);
        }
        addSegment(segments, cursor.consume());
        while (cursor.match(TokenKind.DOT)) {
            if (!cursor.check(TokenKind.IDENTIFIER)) {
                context.expected(cursor.current(), TokenKind.IDENTIFIER);
                break;
            }
            addSegment(segments, cursor.consume());
        }
        return new QualifiedNameSyntax(segments, context.span(first, cursor.previous()));
    }

    private QualifiedNameSyntax parseTypeName() {
        if (cursor.check(TokenKind.KEYWORD_VOID)) {
            Token token = cursor.consume();
            return new QualifiedNameSyntax(List.of("void"), token.span());
        }
        return parseQualifiedName();
    }

    private void addSegment(List<String> segments, Token token) {
        if (segments.size() >= context.limits().maxQualifiedNameSegments()) {
            context.diagnostics().report(
                    DiagnosticCodes.QUALIFIED_NAME_LIMIT_EXCEEDED,
                    token.span(),
                    "Qualified-name segment limit exceeded."
            );
            return;
        }
        segments.add(token.text());
    }

    private QualifiedNameSyntax missingName(Token token) {
        SourceSpan span = context.emptySpan(token);
        return new QualifiedNameSyntax(List.of("<missing>"), span);
    }

    private static SyntaxModifier toModifier(TokenKind kind) {
        return switch (kind) {
            case KEYWORD_PUBLIC -> SyntaxModifier.PUBLIC;
            case KEYWORD_PRIVATE -> SyntaxModifier.PRIVATE;
            case KEYWORD_PROTECTED -> SyntaxModifier.PROTECTED;
            case KEYWORD_OVERRIDE -> SyntaxModifier.OVERRIDE;
            default -> throw new IllegalArgumentException("Token is not a modifier: " + kind);
        };
    }

    static final class ParseContext {
        private final SourceText source;
        private final ProcessingLimits limits;
        private final DiagnosticBag diagnostics;

        ParseContext(SourceText source, ProcessingLimits limits, DiagnosticBag diagnostics) {
            this.source = source;
            this.limits = limits;
            this.diagnostics = diagnostics;
        }

        ProcessingLimits limits() {
            return limits;
        }

        DiagnosticBag diagnostics() {
            return diagnostics;
        }

        SourceSpan span(Token start, Token end) {
            return source.span(start.span().startOffset(), end.span().endOffset());
        }

        SourceSpan span(SourceSpan start, SourceSpan end) {
            return source.span(start.startOffset(), end.endOffset());
        }

        SourceSpan emptySpan(Token token) {
            return source.span(token.span().startOffset(), token.span().startOffset());
        }

        void expected(Token actual, TokenKind expected) {
            diagnostics.report(DiagnosticCodes.EXPECTED_TOKEN, actual.span(),
                    "Expected " + expected + " but found " + actual.kind() + ".");
        }

        void missing(Token actual, String code, String message) {
            diagnostics.report(code, emptySpan(actual), message, Optional.of("Insert the missing delimiter."));
        }

        void unexpected(Token actual, String message) {
            diagnostics.report(DiagnosticCodes.UNEXPECTED_TOKEN, actual.span(), message);
        }
    }
}
