package io.github.kxyr0.echoscript.language.parser;

import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.diagnostic.LanguageDiagnostic;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.language.diagnostic.DiagnosticBag;
import io.github.kxyr0.echoscript.language.diagnostic.DiagnosticCodes;
import io.github.kxyr0.echoscript.language.lexer.EchoScriptLexer;
import io.github.kxyr0.echoscript.language.lexer.LexResult;
import io.github.kxyr0.echoscript.language.lexer.Token;
import io.github.kxyr0.echoscript.language.lexer.TokenKind;
import io.github.kxyr0.echoscript.language.parser.TypeParser.ParseContext;
import io.github.kxyr0.echoscript.language.source.SourceText;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DefaultEchoScriptParser implements EchoScriptParser {
    @Override
    public ParseResult parse(ParseRequest request) {
        Objects.requireNonNull(request, "request");
        SourceText source = new SourceText(request.sourceId(), request.sourceText());
        DiagnosticBag earlyDiagnostics = new DiagnosticBag(source, request.limits());
        if (!request.languageVersion().isV1()) {
            earlyDiagnostics.report(
                    DiagnosticCodes.UNSUPPORTED_LANGUAGE_VERSION,
                    source.span(0, Math.min(1, source.length())),
                    "Unsupported EchoScript language version " + request.languageVersion().major() + "."
            );
            return failed(request, earlyDiagnostics.diagnostics());
        }
        if (source.length() > request.limits().maxSourceLength()) {
            earlyDiagnostics.report(DiagnosticCodes.SOURCE_TOO_LARGE, source.span(0, 0), "Source is too large.");
            return failed(request, earlyDiagnostics.diagnostics());
        }
        return parseV1(request, source);
    }

    private ParseResult parseV1(ParseRequest request, SourceText source) {
        LexResult lexResult = new EchoScriptLexer(source, request.limits()).lex();
        List<LanguageDiagnostic> diagnostics = new ArrayList<>(lexResult.diagnostics());
        if (lexResult.halted()) {
            return failed(request, diagnostics);
        }
        DiagnosticBag parseDiagnostics = new DiagnosticBag(source, request.limits());
        ParseContext context = new ParseContext(source, request.limits(), parseDiagnostics);
        SyntaxParser parser = new SyntaxParser(new TokenCursor(lexResult.tokens()), context);
        Optional<CompilationUnitSyntax> tree = parser.parse();
        diagnostics.addAll(parseDiagnostics.diagnostics());
        boolean successful = diagnostics.isEmpty() && tree.isPresent();
        return new ParseResult(
                request.languageVersion(),
                request.sourceId(),
                successful ? tree : Optional.empty(),
                diagnostics,
                successful
        );
    }

    private ParseResult failed(ParseRequest request, List<LanguageDiagnostic> diagnostics) {
        return new ParseResult(
                request.languageVersion(),
                request.sourceId(),
                Optional.empty(),
                diagnostics,
                false
        );
    }

    static final class TokenCursor {
        private final List<Token> tokens;
        private int position;

        TokenCursor(List<Token> tokens) {
            this.tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens"));
        }

        Token current() {
            return position >= tokens.size() ? tokens.get(tokens.size() - 1) : tokens.get(position);
        }

        Token previous() {
            return tokens.get(Math.max(0, position - 1));
        }

        Token peek(int lookahead) {
            return tokens.get(Math.min(position + lookahead, tokens.size() - 1));
        }

        boolean isAtEnd() {
            return current().is(TokenKind.EOF);
        }

        boolean check(TokenKind kind) {
            return current().is(kind);
        }

        boolean match(TokenKind kind) {
            if (!check(kind)) {
                return false;
            }
            consume();
            return true;
        }

        Token consume() {
            Token token = current();
            if (!isAtEnd()) {
                position++;
            }
            return token;
        }
    }
}
