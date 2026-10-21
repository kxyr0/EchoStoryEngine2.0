package io.github.kxyr0.echoscript.language.parser;

import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.language.diagnostic.DiagnosticCodes;
import io.github.kxyr0.echoscript.language.lexer.Token;
import io.github.kxyr0.echoscript.language.lexer.TokenKind;
import io.github.kxyr0.echoscript.language.parser.DefaultEchoScriptParser.TokenCursor;
import io.github.kxyr0.echoscript.language.parser.TypeParser.ParseContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class ArgumentListParser {
    private final TokenCursor cursor;
    private final ParseContext context;
    private final ExpressionParser expressions;

    ArgumentListParser(TokenCursor cursor, ParseContext context, ExpressionParser expressions) {
        this.cursor = cursor;
        this.context = context;
        this.expressions = expressions;
    }

    List<ArgumentSyntax> parse(int depth) {
        List<ArgumentSyntax> arguments = new ArrayList<>();
        Set<String> named = new HashSet<>();
        boolean sawNamed = false;
        while (!cursor.check(TokenKind.CLOSE_PAREN) && !cursor.isAtEnd()) {
            if (arguments.size() >= context.limits().maxInvocationArguments()) {
                reportArgumentLimit();
                break;
            }
            ArgumentSyntax argument = parseArgument(depth + 1, sawNamed, named);
            sawNamed = sawNamed || argument.name().isPresent();
            arguments.add(argument);
            if (!cursor.match(TokenKind.COMMA)) {
                break;
            }
        }
        expectClosingParenthesis();
        return arguments;
    }

    private ArgumentSyntax parseArgument(int depth, boolean sawNamed, Set<String> named) {
        Token start = cursor.current();
        if (cursor.check(TokenKind.IDENTIFIER) && cursor.peek(1).is(TokenKind.COLON)) {
            return parseNamedArgument(depth, named);
        }
        if (sawNamed) {
            context.diagnostics().report(
                    DiagnosticCodes.POSITIONAL_AFTER_NAMED_ARGUMENT,
                    start.span(),
                    "Positional argument after named arguments."
            );
        }
        ExpressionSyntax value = expressions.parseExpression(depth + 1);
        return new ArgumentSyntax(Optional.empty(), value, value.span());
    }

    private ArgumentSyntax parseNamedArgument(int depth, Set<String> named) {
        Token name = cursor.consume();
        cursor.consume();
        if (!named.add(name.text())) {
            context.diagnostics().report(
                    DiagnosticCodes.DUPLICATE_NAMED_ARGUMENT,
                    name.span(),
                    "Duplicate named argument."
            );
        }
        if (cursor.check(TokenKind.COMMA) || cursor.check(TokenKind.CLOSE_PAREN) || cursor.isAtEnd()) {
            context.diagnostics().report(
                    DiagnosticCodes.MALFORMED_NAMED_ARGUMENT,
                    name.span(),
                    "Malformed named argument."
            );
            return new ArgumentSyntax(
                    Optional.of(name.text()),
                    new NameExpressionSyntax("<error>", context.emptySpan(cursor.current())),
                    context.span(name, cursor.current())
            );
        }
        ExpressionSyntax value = expressions.parseExpression(depth + 1);
        return new ArgumentSyntax(Optional.of(name.text()), value, context.span(name, cursor.previous()));
    }

    private void reportArgumentLimit() {
        context.diagnostics().report(
                DiagnosticCodes.ARGUMENT_LIMIT_EXCEEDED,
                cursor.current().span(),
                "Too many invocation arguments."
        );
    }

    private void expectClosingParenthesis() {
        if (!cursor.match(TokenKind.CLOSE_PAREN)) {
            context.missing(
                    cursor.current(),
                    DiagnosticCodes.MISSING_CLOSING_PARENTHESIS,
                    "Missing closing parenthesis."
            );
        }
    }
}
